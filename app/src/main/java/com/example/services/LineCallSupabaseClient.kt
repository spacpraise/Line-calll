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
data class AccountLookupResult(val exists: Boolean, val displayName: String = "", val avatarUrl: String? = null)

data class RealtimeCredentials(val supabaseUrl: String, val anonKey: String, val accessToken: String)

/** A verified email+phone match for the SAME existing account, ready to set a new password. */
data class PasswordRecoveryMatch(val tokenHash: String)

class LineCallSupabaseClient(context: Context) {
  private val http = OkHttpClient.Builder().build()
  private val secureStore = SecureSessionStore(context.applicationContext)
  private val jsonType = "application/json; charset=utf-8".toMediaType()

  val isConfigured: Boolean
    get() = BackendConfig.SUPABASE_URL.isNotBlank() &&
      BackendConfig.SUPABASE_ANON_KEY.isNotBlank()

  suspend fun checkEmailExists(email: String): Result<AccountLookupResult> = withContext(Dispatchers.IO) {
    runCatching {
      check(isConfigured) { "Supabase is not configured." }
      val response = execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/functions/v1/check-account")
          .header("apikey", BackendConfig.SUPABASE_ANON_KEY)
          .post(JSONObject().put("email", email.trim().lowercase()).toString().toRequestBody(jsonType))
          .build()
      )
      AccountLookupResult(
        exists = response.optBoolean("exists"),
        displayName = response.optString("display_name").takeUnless { it == "null" }.orEmpty(),
        avatarUrl = response.optString("avatar_url").takeUnless { it.isBlank() || it == "null" }
      )
    }
  }

  suspend fun signUpWithPassword(email: String, password: String, phoneNumber: String): Result<UserAccount> = withContext(Dispatchers.IO) {
    runCatching {
      check(isConfigured) { "Supabase is not configured." }
      require(password.length >= 8) { "Use at least 8 characters for your password." }
      val response = execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/functions/v1/register-account")
          .header("apikey", BackendConfig.SUPABASE_ANON_KEY)
          .post(JSONObject().put("email", email.trim()).put("password", password).put("phone_number", phoneNumber).toString().toRequestBody(jsonType))
          .build()
      )
      if (response.optString("access_token").isBlank() || response.optString("refresh_token").isBlank()) {
        throw IllegalStateException("Registration did not return a sign-in session. Please try again.")
      }
      storeSession(response)
      getOrCreateProfileBlocking(getAuthenticatedUserBlocking())
    }
  }

  /**
   * Accepts either an email address or an E.164 phone number in [identifier],
   * matching whichever the account was registered or last set with.
   */
  suspend fun signInWithPassword(identifier: String, password: String): Result<UserAccount> = withContext(Dispatchers.IO) {
    runCatching {
      check(isConfigured) { "Supabase is not configured." }
      val clean = identifier.trim()
      val response = if (clean.contains("@")) {
        execute(
          Request.Builder()
            .url("${BackendConfig.SUPABASE_URL}/auth/v1/token?grant_type=password")
            .header("apikey", BackendConfig.SUPABASE_ANON_KEY)
            .post(JSONObject().put("email", clean).put("password", password).toString().toRequestBody(jsonType))
            .build()
        )
      } else {
        require(clean.startsWith("+") && clean.drop(1).matches(Regex("^[1-9][0-9]{6,14}$"))) {
          "Enter your email address, or your phone number with its country code, for example +2348012345678."
        }
        execute(
          Request.Builder()
            .url("${BackendConfig.SUPABASE_URL}/functions/v1/phone-login")
            .header("apikey", BackendConfig.SUPABASE_ANON_KEY)
            .post(
              JSONObject().put("phone_number", clean).put("password", password)
                .toString().toRequestBody(jsonType)
            )
            .build()
        )
      }
      storeSession(response)
      val authUser = getAuthenticatedUserBlocking()
      getOrCreateProfileBlocking(authUser)
    }
  }

  fun hasStoredSession(): Boolean = secureStore.hasValidStoredRefreshToken()

  suspend fun restoreSession(): UserAccount? = withContext(Dispatchers.IO) {
    val refreshToken = secureStore.get("refresh_token") ?: return@withContext null
    try {
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
    } catch (error: HttpStatusException) {
      // A revoked/deleted/expired account is genuinely signed out. Network failures
      // are allowed to propagate so startup can retry without showing onboarding.
      if (error.statusCode == 400 || error.statusCode == 401) {
        secureStore.clear()
        null
      } else {
        throw error
      }
    }
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

  suspend fun saveFcmToken(userId: String, token: String) = withContext(Dispatchers.IO) {
    require(token.isNotBlank()) { "FCM token is empty." }
    execute(
      Request.Builder()
        .url("${BackendConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.$userId")
        .headers(userHeaders())
        .header("Prefer", "return=minimal")
        .patch(JSONObject().put("fcm_token", token).toString().toRequestBody(jsonType))
        .build()
    )
  }

  suspend fun clearFcmToken(userId: String) = withContext(Dispatchers.IO) {
    execute(
      Request.Builder()
        .url("${BackendConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.$userId")
        .headers(userHeaders())
        .header("Prefer", "return=minimal")
        .patch(JSONObject().put("fcm_token", JSONObject.NULL).toString().toRequestBody(jsonType))
        .build()
    )
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

  /**
   * Build-phase behavior: email changes apply immediately, matching how phone
   * number changes already work. No confirmation link is required, since
   * this account pool has no verification step for now.
   */
  suspend fun requestEmailChange(newEmail: String): Result<UserAccount> = withContext(Dispatchers.IO) {
    runCatching {
      val cleanEmail = newEmail.trim().lowercase()
      require(android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
        "Enter a valid email address."
      }
      // Supabase Auth's own "Confirm email" / "Secure email change" project
      // settings can still require confirmation independently of this call;
      // those are controlled from the Supabase dashboard, not from here.
      execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/auth/v1/user")
          .headers(userHeaders())
          .put(JSONObject().put("email", cleanEmail).toString().toRequestBody(jsonType))
          .build()
      )
      execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.${getAuthenticatedUserBlocking().getString("id")}")
          .headers(userHeaders())
          .patch(JSONObject().put("email", cleanEmail).toString().toRequestBody(jsonType))
          .build()
      )
      getOrCreateProfileBlocking(getAuthenticatedUserBlocking())
    }
  }

  suspend fun saveAvatar(user: UserAccount, jpegBytes: ByteArray): Result<UserAccount> = withContext(Dispatchers.IO) {
    runCatching {
      require(jpegBytes.isNotEmpty()) { "The selected photo could not be read." }
      val objectPath = "${user.id}/profile.jpg"
      execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/storage/v1/object/avatars/$objectPath")
          .headers(userHeaders())
          .header("Content-Type", "image/jpeg")
          .header("x-upsert", "true")
          .post(jpegBytes.toRequestBody("image/jpeg".toMediaType()))
          .build()
      )
      val publicUrl = "${BackendConfig.SUPABASE_URL}/storage/v1/object/public/avatars/$objectPath?v=${System.currentTimeMillis()}"
      execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.${user.id}")
          .headers(userHeaders())
          .patch(JSONObject().put("avatar_url", publicUrl).toString().toRequestBody(jsonType))
          .build()
      )
      user.copy(avatarUrl = publicUrl)
    }
  }

  suspend fun discoverContacts(phoneNumbers: List<String>, emails: List<String>): List<JSONObject> = withContext(Dispatchers.IO) {
    val payload = JSONObject().put("action", "lookup")
      .put("phone_numbers", JSONArray(phoneNumbers)).put("emails", JSONArray(emails))
    val response = execute(Request.Builder()
      .url("${BackendConfig.SUPABASE_URL}/functions/v1/contact-discovery")
      .headers(userHeaders()).post(payload.toString().toRequestBody(jsonType)).build())
    val contacts = response.optJSONArray("contacts") ?: JSONArray()
    (0 until contacts.length()).mapNotNull { contacts.optJSONObject(it) }
  }

  suspend fun cleanupOldCallSignalingData() = withContext(Dispatchers.IO) {
    val payload = JSONObject().put("action", "cleanup")
    execute(Request.Builder().url("${BackendConfig.SUPABASE_URL}/functions/v1/contact-discovery")
      .headers(userHeaders()).post(payload.toString().toRequestBody(jsonType)).build())
  }

  suspend fun updatePresence(deviceId: String, state: String) = withContext(Dispatchers.IO) {
    val payload = JSONObject().put("action", "presence").put("device_id", deviceId)
      .put("state", if (state == "in_call") "in_call" else "online")
    execute(Request.Builder().url("${BackendConfig.SUPABASE_URL}/functions/v1/contact-discovery")
      .headers(userHeaders()).post(payload.toString().toRequestBody(jsonType)).build())
  }

  suspend fun clearPresence(deviceId: String) = withContext(Dispatchers.IO) {
    val payload = JSONObject().put("action", "offline").put("device_id", deviceId)
    execute(Request.Builder().url("${BackendConfig.SUPABASE_URL}/functions/v1/contact-discovery")
      .headers(userHeaders()).post(payload.toString().toRequestBody(jsonType)).build())
  }
  suspend fun realtimeCredentials(): RealtimeCredentials = withContext(Dispatchers.IO) {
    check(isConfigured) { "Supabase is not configured." }
    RealtimeCredentials(
      supabaseUrl = BackendConfig.SUPABASE_URL,
      anonKey = BackendConfig.SUPABASE_ANON_KEY,
      accessToken = freshAccessTokenBlocking()
    )
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

  /**
   * Confirms the given email and phone number belong to the SAME existing
   * account, with no email sent and nothing about the account changed. On a
   * match, returns a recovery token the app exchanges immediately (via
   * [verifyAuthCallback]-style /auth/v1/verify) so the user lands directly on
   * the in-app "choose a new password" screen -- no email round-trip, and the
   * account's id, contacts and call history are untouched throughout.
   */
  suspend fun requestPasswordRecovery(email: String, phoneNumber: String): Result<PasswordRecoveryMatch> = withContext(Dispatchers.IO) {
    runCatching {
      check(isConfigured) { "Supabase is not configured." }
      val response = execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/functions/v1/password-recovery")
          .header("apikey", BackendConfig.SUPABASE_ANON_KEY)
          .post(
            JSONObject().put("email", email.trim()).put("phone_number", phoneNumber.trim())
              .toString().toRequestBody(jsonType)
          )
          .build()
      )
      val tokenHash = response.optString("token_hash").takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("Could not verify these account details. Please try again.")
      PasswordRecoveryMatch(tokenHash)
    }
  }

  suspend fun acceptAuthSession(accessToken: String, refreshToken: String, expiresIn: Long): Result<UserAccount> = withContext(Dispatchers.IO) {
    runCatching {
      check(isConfigured) { "Supabase is not configured." }
      require(accessToken.isNotBlank() && refreshToken.isNotBlank()) { "The authentication session is incomplete." }
      storeSession(
        JSONObject()
          .put("access_token", accessToken)
          .put("refresh_token", refreshToken)
          .put("expires_in", expiresIn.coerceAtLeast(60L))
      )
      getOrCreateProfileBlocking(getAuthenticatedUserBlocking())
    }
  }

  suspend fun verifyAuthCallback(tokenHash: String, type: String): Result<UserAccount> = withContext(Dispatchers.IO) {
    runCatching {
      check(isConfigured) { "Supabase is not configured." }
      require(type in setOf("email_change", "signup", "magiclink", "invite", "email")) {
        "Unsupported authentication link type."
      }
      val session = execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/auth/v1/verify")
          .header("apikey", BackendConfig.SUPABASE_ANON_KEY)
          .post(
            JSONObject().put("type", type).put("token_hash", tokenHash)
              .toString().toRequestBody(jsonType)
          )
          .build()
      )
      storeSession(session)
      getOrCreateProfileBlocking(getAuthenticatedUserBlocking())
    }
  }

  suspend fun completePasswordRecovery(tokenHash: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      check(isConfigured) { "Supabase is not configured." }
      require(newPassword.length >= 8) { "Use at least 8 characters for your new password." }
      if (tokenHash.isNotBlank()) {
        val session = execute(
          Request.Builder()
            .url("${BackendConfig.SUPABASE_URL}/auth/v1/verify")
            .header("apikey", BackendConfig.SUPABASE_ANON_KEY)
            .post(
              JSONObject().put("type", "recovery").put("token_hash", tokenHash)
                .toString().toRequestBody(jsonType)
            )
            .build()
        )
        storeSession(session)
      } else {
        check(!secureStore.get("access_token").isNullOrBlank()) {
          "The password reset session is missing. Start the reset again."
        }
      }
      execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/auth/v1/user")
          .headers(userHeaders())
          .put(JSONObject().put("password", newPassword).toString().toRequestBody(jsonType))
          .build()
      )
      // Keep the session signed in: the user set a new password for their
      // existing account and should land in the app, not back at login.
    }
  }

  suspend fun refreshAuthenticatedProfile(): Result<UserAccount> = withContext(Dispatchers.IO) {
    runCatching { getOrCreateProfileBlocking(getAuthenticatedUserBlocking()) }
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
    val profilePhone = row.optString("phone_number").takeIf { it.isNotBlank() && it != "null" }.orEmpty()
    val profileName = row.optString("display_name").takeIf { it.isNotBlank() && it != "null" }.orEmpty()
    val profileEmail = row.optString("email", email).ifBlank { email }
    if (email.isNotBlank() && profileEmail != email) {
      execute(
        Request.Builder()
          .url("${BackendConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.$id")
          .headers(userHeaders())
          .patch(JSONObject().put("email", email).toString().toRequestBody(jsonType))
          .build()
      )
    }
    return UserAccount(
      id = id,
      email = email.ifBlank { profileEmail },
      phoneNumber = profilePhone,
      isPhoneVerified = row.optBoolean("phone_verified", false),
      displayName = profileName,
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
            json.optString("msg").ifBlank {
              json.optString("error").ifBlank { json.optString("error_description").ifBlank { raw } }
            }
          }
        }.getOrDefault(raw)
        throw HttpStatusException(response.code, message.ifBlank { "Supabase request failed (${response.code})." })
      }
      if (raw.isBlank()) return JSONObject()
      return if (raw.trimStart().startsWith("[")) JSONObject().put("data", JSONArray(raw)) else JSONObject(raw)
    }
  }
}

private class HttpStatusException(val statusCode: Int, message: String) : IOException(message)

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
    check(prefs.edit().putString(name, Base64.encodeToString(combined, Base64.NO_WRAP)).commit()) {
      "Could not securely save the sign-in session."
    }
  }

  fun get(name: String): String? = runCatching {
    val combined = Base64.decode(prefs.getString(name, null) ?: return null, Base64.NO_WRAP)
    val iv = combined.copyOfRange(0, 12)
    val encrypted = combined.copyOfRange(12, combined.size)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
    String(cipher.doFinal(encrypted), Charsets.UTF_8)
  }.getOrNull()

  fun hasValidStoredRefreshToken(): Boolean {
    // This is called while the first screen is being created. Only inspect the
    // inexpensive preference index here; Android Keystore decryption belongs
    // in restoreSession(), which already runs on Dispatchers.IO.
    return prefs.contains("refresh_token")
  }

  fun remove(name: String) {
    prefs.edit().remove(name).apply()
  }

  fun clear() {
    prefs.edit().clear().commit()
  }
}
