package com.example.services

import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
  data object Unauthenticated : AuthState()
  data class Authenticated(val user: UserAccount) : AuthState()
}

interface AuthService {
  val authState: StateFlow<AuthState>
  suspend fun register(email: String, password: String, phoneNumber: String): Result<UserAccount>
  suspend fun signIn(identifier: String, password: String): Result<UserAccount>
  suspend fun restoreSession(): UserAccount?
  suspend fun signOut()
}

/** Supabase email-and-password authentication. No email-link callback is used. */
class LineCallAuthService(
  private val supabase: LineCallSupabaseClient
) : AuthService {
  private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
  override val authState: StateFlow<AuthState> = _authState.asStateFlow()

  private var currentUser: UserAccount? = null

  override suspend fun register(email: String, password: String, phoneNumber: String): Result<UserAccount> {
    val cleanEmail = email.trim()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      return Result.failure(IllegalArgumentException("Please enter a valid email address."))
    }
    if (password.length < 8) {
      return Result.failure(IllegalArgumentException("Use at least 8 characters for your password."))
    }
    val result = supabase.signUpWithPassword(cleanEmail, password, phoneNumber)
    result.onSuccess { user ->
      currentUser = user
      _authState.value = AuthState.Authenticated(user)
    }
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

  override suspend fun restoreSession(): UserAccount? {
    val restoredUser = supabase.restoreSession()
    // Keep an already authenticated in-memory user if restoration completes later.
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
