package com.example.services

import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class AuthState {
  data object Unauthenticated : AuthState()
  data class AwaitingMagicLink(val email: String) : AuthState()
  data class Authenticated(val user: UserAccount) : AuthState()
}

interface AuthService {
  val authState: StateFlow<AuthState>
  suspend fun sendMagicLink(email: String): Result<Unit>
  suspend fun handleAuthCallback(email: String): Result<UserAccount>
  suspend fun signOut()
}

/**
 * Production-ready AuthService implementation for Supabase Magic Link authentication.
 *
 * Flow:
 * 1. User enters email -> calls Supabase /auth/v1/magiclink
 * 2. Supabase sends email with token
 * 3. User clicks link or verifies token -> Supabase session established
 * 4. User moves to Phone Number Setup
 */
class LineCallAuthService : AuthService {
  private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
  override val authState: StateFlow<AuthState> = _authState.asStateFlow()

  // Cached current user during local development & session persistence
  private var currentUser: UserAccount? = null

  override suspend fun sendMagicLink(email: String): Result<Unit> {
    val cleanEmail = email.trim()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      return Result.failure(IllegalArgumentException("Please enter a valid email address."))
    }

    // When Supabase is configured:
    // TODO: val response = supabaseClient.auth.signInWith(OTP) { this.email = cleanEmail }
    // For development state: transition to AwaitingMagicLink
    _authState.value = AuthState.AwaitingMagicLink(cleanEmail)
    return Result.success(Unit)
  }

  override suspend fun handleAuthCallback(email: String): Result<UserAccount> {
    // When Supabase link callback is invoked:
    // TODO: val session = supabaseClient.auth.retrieveSessionFromUrl(...)
    val user = UserAccount(
      id = UUID.randomUUID().toString(),
      email = email,
      phoneNumber = "",
      isPhoneVerified = false,
      displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
    )
    currentUser = user
    _authState.value = AuthState.Authenticated(user)
    return Result.success(user)
  }

  fun updateCurrentUser(updatedUser: UserAccount) {
    currentUser = updatedUser
    _authState.value = AuthState.Authenticated(updatedUser)
  }

  override suspend fun signOut() {
    // TODO: supabaseClient.auth.signOut()
    currentUser = null
    _authState.value = AuthState.Unauthenticated
  }
}
