package com.example.services

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.data.model.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small REST client for Supabase Auth, PostgREST and Edge Functions.
 * The publishable key is intentionally a client-side key; access is restricted by RLS.
 */
class LineCallSupabaseClient(context: Context) {
  private val http = OkHttpClient.Builder().build()
  private val secureStore = SecureSessionStore(context.applicationContext)
  private val jsonType = "application/json; charset=utf-8".toMediaType()

  val isConfigured: Boolean
    get() = BackendConfig.SUPABASE_URL.isNotBlank() &&
      BackendConfig.SUPABASE_ANON_KEY.isNotBlank()

  suspend fun signUpWithPassword(email: String, password: String): Result<UserAccount> = withContext(Dispatchers.IO) {
    runCatching {
      check(isConfigured) { "Supabase is not configured." }
      require(password.length >= 8) { "Use at least 8 characters for your password." }
      val response = execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/auth/v1/signup")
          .header("apikey", BackendConfig.SUPABASE_ANON_KEY)
          .post(JSONObject().put("email", email.trim()).put("password", password).toString().toRequestBody(jsonType))
          .build()
      )
      if (response.optString("access_token").isBlank() || response.optString("refresh_token").isBlank()) {
        throw IllegalStateException("Email confirmation is still enabled in Supabase. Disable it in Authentication → Providers → Email.")
      }
      storeSession(response)
      getOrCreateProfileBlocking(getAuthenticatedUserBlocking())
    }
  }

  suspend fun signInWithPassword(identifier: String, password: String): Result<UserAccount> = withContext(Dispatchers.IO) {
    runCatching {
      check(isConfigured) { "Supabase is not configured." }
      val clean = identifier.trim()
      if (!clean.contains("@")) {
        throw IllegalStateException("Phone-number login is not enabled yet. Please use your email address for now.")
      }
      val response = execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/auth/v1/token?grant_type=password")
          .header("apikey", BackendConfig.SUPABASE_ANON_KEY)
          .post(JSONObject().put("email", clean).put("password", password).toString().toRequestBody(jsonType))
          .build()
      )
      storeSession(response)
      val authUser = getAuthenticatedUserBlocking()
      getOrCreateProfileBlocking(authUser)
    }
  }

  suspend fun restoreSession(): UserAccount? = withContext(Dispatchers.IO) {
    runCatching {
      val refreshToken = secureStore.get("refresh_token") ?: return@runCatching null
      val expiry = secureStore.get("expires_at")?.toLongOrNull() ?: 0L
      if (expiry <= System.currentTimeMillis() / 1000 + 60) {
        val refreshed = execute(
          Request.Builder()
            .url("${BackendConfig.SUPABASE_URL}/auth/v1/token?grant_type=refresh_token")
            .header("apikey", BackendConfig.SUPABASE_ANON_KEY)
            .post(JSONObject().put("refresh_token", refreshToken).toString().toRequestBody(jsonType))
            .build()
        )
        storeSession(refreshed)
      }
      val authUser = getAuthenticatedUserBlocking()
      getOrCreateProfileBlocking(authUser)
    }.getOrNull()
  }

  suspend fun savePhoneNumber(user: UserAccount, phoneNumber: String): Result<UserAccount> = withContext(Dispatchers.IO) {
    runCatching {
      val payload = JSONObject().put("phone_number", phoneNumber)
      execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.${user.id}")
          .headers(userHeaders())
          .patch(payload.toString().toRequestBody(jsonType))
          .build()
      )
      user.copy(phoneNumber = phoneNumber, isPhoneVerified = false, isRegisteredOnBackend = true)
    }
  }

  suspend fun saveDisplayName(user: UserAccount, displayName: String): Result<UserAccount> = withContext(Dispatchers.IO) {
    runCatching {
      val payload = JSONObject().put("display_name", displayName)
      execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.${user.id}")
          .headers(userHeaders())
          .patch(payload.toString().toRequestBody(jsonType))
          .build()
      )
      user.copy(displayName = displayName)
    }
  }

  suspend fun callFunction(payload: JSONObject): JSONObject = withContext(Dispatchers.IO) {
    val accessToken = freshAccessTokenBlocking()
    execute(
      Request.Builder()
        .url("${BackendConfig.SUPABASE_URL}/functions/v1/call-control")
        .headers(userHeaders(accessToken))
        .post(payload.toString().toRequestBody(jsonType))
        .build()
    )
  }

  suspend fun signOut() = withContext(Dispatchers.IO) {
    runCatching {
      val access = secureStore.get("access_token")
      if (!access.isNullOrBlank()) {
        execute(
          Request.Builder()
            .url("${BackendConfig.SUPABASE_URL}/auth/v1/logout")
            .headers(userHeaders(access))
            .post(ByteArray(0).toRequestBody(null))
            .build()
        )
      }
    }
    secureStore.clear()
  }

  private fun getAuthenticatedUserBlocking(): JSONObject {
    val token = freshAccessTokenBlocking()
    return execute(
      Request.Builder()
        .url("${BackendConfig.SUPABASE_URL}/auth/v1/user")
        .headers(userHeaders(token))
        .get()
        .build()
    )
  }

  private fun getOrCreateProfileBlocking(authUser: JSONObject): UserAccount {
    val id = authUser.getString("id")
    val email = authUser.optString("email")
    val queryUrl = "${BackendConfig.SUPABASE_URL}/rest/v1/profiles?select=id,email,phone_number,phone_verified,display_name,avatar_url&id=eq.$id&limit=1"
    val rows = execute(
      Request.Builder().url(queryUrl).headers(userHeaders()).get().build()
    )
    val profileRows = rows.optJSONArray("data")
    val row = if (profileRows != null && profileRows.length() > 0) profileRows.getJSONObject(0) else null
    if (row == null) {
      val defaultName = email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
      execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/rest/v1/profiles")
          .headers(userHeaders())
          .header("Prefer", "return=minimal")
          .post(
            JSONObject()
              .put("id", id)
              .put("email", email)
              .put("display_name", defaultName)
              .toString().toRequestBody(jsonType)
          )
          .build()
      )
      return UserAccount(id, email, "", false, defaultName, null, true)
    }
    return UserAccount(
      id = id,
      email = row.optString("email", email),
      phoneNumber = row.optString("phone_number", ""),
      isPhoneVerified = row.optBoolean("phone_verified", false),
      displayName = row.optString("display_name", ""),
      avatarUrl = row.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" },
      isRegisteredOnBackend = true
    )
  }

  private fun freshAccessTokenBlocking(): String {
    val current = secureStore.get("access_token")
      ?: throw IllegalStateException("Your session has expired. Please sign in again.")
    val expiresAt = secureStore.get("expires_at")?.toLongOrNull() ?: 0L
    if (expiresAt > System.currentTimeMillis() / 1000 + 60) return current

    val refresh = secureStore.get("refresh_token")
      ?: throw IllegalStateException("Your session has expired. Please sign in again.")
    val response = execute(
      Request.Builder()
        .url("${BackendConfig.SUPABASE_URL}/auth/v1/token?grant_type=refresh_token")
        .header("apikey", BackendConfig.SUPABASE_ANON_KEY)
        .post(JSONObject().put("refresh_token", refresh).toString().toRequestBody(jsonType))
        .build()
    )
    storeSession(response)
    return response.getString("access_token")
  }

  private fun storeSession(session: JSONObject) {
    secureStore.put("access_token", session.getString("access_token"))
    secureStore.put("refresh_token", session.getString("refresh_token"))
    val expiresAt = session.optLong("expires_at", 0L).takeIf { it > 0L }
      ?: (System.currentTimeMillis() / 1000 + session.optLong("expires_in", 3600L))
    secureStore.put("expires_at", expiresAt.toString())
  }

  private fun userHeaders(token: String? = null): okhttp3.Headers {
    val access = token ?: freshAccessTokenBlocking()
    return okhttp3.Headers.Builder()
      .add("apikey", BackendConfig.SUPABASE_ANON_KEY)
      .add("Authorization", "Bearer $access")
      .add("Content-Type", "application/json")
      .build()
  }

  private fun execute(request: Request): JSONObject {
    http.newCall(request).execute().use { response ->
      val raw = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        val message = runCatching {
          val json = JSONObject(raw)
          json.optString("message").ifBlank {
            json.optString("msg").ifBlank { json.optString("error_description").ifBlank { raw } }
          }
        }.getOrDefault(raw)
        throw IOException(message.ifBlank { "Supabase request failed (${response.code})." })
      }
      if (raw.isBlank()) return JSONObject()
      return if (raw.trimStart().startsWith("[")) JSONObject().put("data", JSONArray(raw)) else JSONObject(raw)
    }
  }
}

/** Encrypts session material with a key held in Android Keystore. */
private class SecureSessionStore(context: Context) {
  private val prefs = context.getSharedPreferences("line_call_secure_session", Context.MODE_PRIVATE)
  private val alias = "line_call_session_aes_key"

  private fun key(): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val existing = keyStore.getKey(alias, null) as? SecretKey
    if (existing != null) return existing
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
      KeyGenParameterSpec.Builder(
        alias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build()
    )
    return generator.generateKey()
  }

  fun put(name: String, value: String) {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key())
    val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
    val combined = cipher.iv + encrypted
    prefs.edit().putString(name, Base64.encodeToString(combined, Base64.NO_WRAP)).apply()
  }

  fun get(name: String): String? = runCatching {
    val combined = Base64.decode(prefs.getString(name, null) ?: return null, Base64.NO_WRAP)
    val iv = combined.copyOfRange(0, 12)
    val encrypted = combined.copyOfRange(12, combined.size)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
    String(cipher.doFinal(encrypted), Charsets.UTF_8)
  }.getOrNull()

  fun remove(name: String) {
    prefs.edit().remove(name).apply()
  }

  fun clear() {
    prefs.edit().clear().apply()
  }
}
