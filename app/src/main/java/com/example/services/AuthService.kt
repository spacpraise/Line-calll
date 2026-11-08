package com.example.services

import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
  data object Unauthenticated : AuthState()
  data class AwaitingMagicLink(val email: String) : AuthState()
  data class Authenticated(val user: UserAccount) : AuthState()
}

interface AuthService {
  val authState: StateFlow<AuthState>
  suspend fun sendMagicLink(email: String): Result<Unit>
  suspend fun signIn(identifier: String, password: String): Result<UserAccount>
  suspend fun sendPasswordRecovery(email: String): Result<Unit>
  suspend fun updatePassword(password: String): Result<Unit>
  fun pendingAuthFlow(): String?
  suspend fun handleAuthCallback(callbackUrl: String): Result<UserAccount>
  suspend fun restoreSession(): UserAccount?
  suspend fun signOut()
}

/**
 * Supabase passwordless email authentication using a PKCE magic link.
 * The callback deep link exchanges the one-time authorization code for a session.
 */
class LineCallAuthService(
  private val supabase: LineCallSupabaseClient
) : AuthService {
  private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
  override val authState: StateFlow<AuthState> = _authState.asStateFlow()

  private var currentUser: UserAccount? = null

  override suspend fun sendMagicLink(email: String): Result<Unit> {
    val cleanEmail = email.trim()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      return Result.failure(IllegalArgumentException("Please enter a valid email address."))
    }
    val result = supabase.sendMagicLink(cleanEmail)
    result.onSuccess { _authState.value = AuthState.AwaitingMagicLink(cleanEmail) }
    return result
  }

  override suspend fun signIn(identifier: String, password: String): Result<UserAccount> {
    val result = supabase.signInWithPassword(identifier, password)
    result.onSuccess { user ->
      currentUser = user
      _authState.value = AuthState.Authenticated(user)
    }
    return result
  }

  override suspend fun sendPasswordRecovery(email: String): Result<Unit> {
    val cleanEmail = email.trim()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      return Result.failure(IllegalArgumentException("Please enter a valid email address."))
    }
    return supabase.sendPasswordRecovery(cleanEmail)
  }

  override suspend fun updatePassword(password: String): Result<Unit> =
    supabase.updatePassword(password)

  override fun pendingAuthFlow(): String? = supabase.pendingAuthFlow()

  override suspend fun handleAuthCallback(callbackUrl: String): Result<UserAccount> {
    val result = supabase.completeMagicLink(callbackUrl)
    result.onSuccess { user ->
      currentUser = user
      _authState.value = AuthState.Authenticated(user)
    }
    return result
  }

  override suspend fun restoreSession(): UserAccount? {
    val restoredUser = supabase.restoreSession()
    // A magic-link callback can finish while session restoration is still in flight.
    // Never let a late restore result overwrite the newly authenticated callback user.
    val resolvedUser = currentUser ?: restoredUser
    if (currentUser == null) currentUser = restoredUser
    _authState.value = if (resolvedUser == null) {
      AuthState.Unauthenticated
    } else {
      AuthState.Authenticated(resolvedUser)
    }
    return resolvedUser
  }

  fun updateCurrentUser(updatedUser: UserAccount) {
    currentUser = updatedUser
    _authState.value = AuthState.Authenticated(updatedUser)
  }

  override suspend fun signOut() {
    supabase.signOut()
    currentUser = null
    _authState.value = AuthState.Unauthenticated
  }
}
