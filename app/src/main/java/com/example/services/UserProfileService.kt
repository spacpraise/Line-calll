package com.example.services

import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

sealed class RegistrationStatus {
  data object NotChecked : RegistrationStatus()
  data class Registered(val e164: String, val name: String?) : RegistrationStatus()
  data class NotRegistered(val e164: String) : RegistrationStatus()
  data class BackendUnavailable(val e164: String, val message: String) : RegistrationStatus()
}

class UserProfileService(
  private val authService: LineCallAuthService,
  private val supabase: LineCallSupabaseClient
) {
  private val _currentUser = MutableStateFlow<UserAccount?>(null)
  val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

  fun setUser(user: UserAccount) {
    _currentUser.value = user
    authService.updateCurrentUser(user)
  }

  fun clearUser() {
    _currentUser.value = null
  }

  fun formatToE164(dialCode: String, nationalNumber: String): String =
    PhoneNumberFormatter.formatToE164(dialCode, nationalNumber)

  fun validateE164(e164: String): Boolean = PhoneNumberFormatter.isValidE164(e164)

  /** Saves the number without claiming that its owner has been verified. */
  suspend fun registerPhoneNumber(e164PhoneNumber: String): Result<UserAccount> {
    if (!validateE164(e164PhoneNumber)) {
      return Result.failure(
        IllegalArgumentException("Enter a valid international phone number, including its country code.")
      )
    }
    val current = _currentUser.value
      ?: return Result.failure(IllegalStateException("Sign in with your email before adding a phone number."))
    val result = supabase.savePhoneNumber(current, e164PhoneNumber)
    result.onSuccess(::setUser)
    return result
  }

  suspend fun updateProfile(displayName: String, avatarUrl: String? = null): Result<UserAccount> {
    val current = _currentUser.value
      ?: return Result.failure(IllegalStateException("No signed-in account was found."))
    val cleanName = displayName.trim().ifBlank { current.displayName }
    val result = supabase.saveDisplayName(current.copy(avatarUrl = avatarUrl ?: current.avatarUrl), cleanName)
    result.onSuccess(::setUser)
    return result
  }

  suspend fun refreshCurrentUser(): Result<UserAccount> {
    val result = supabase.refreshAuthenticatedProfile()
    result.onSuccess(::setUser)
    return result
  }

  suspend fun updatePhoneNumber(e164PhoneNumber: String): Result<UserAccount> =
    registerPhoneNumber(e164PhoneNumber)

  suspend fun updateEmail(newEmail: String): Result<Unit> =
    supabase.requestEmailChange(newEmail)

  suspend fun updateAvatar(jpegBytes: ByteArray): Result<UserAccount> {
    val current = _currentUser.value
      ?: return Result.failure(IllegalStateException("No signed-in account was found."))
    val result = supabase.saveAvatar(current, jpegBytes)
    result.onSuccess(::setUser)
    return result
  }

  suspend fun checkTargetUserRegistration(e164PhoneNumber: String): RegistrationStatus {
    if (!validateE164(e164PhoneNumber)) return RegistrationStatus.NotRegistered(e164PhoneNumber)
    return try {
      val response = supabase.callFunction(
        JSONObject().put("action", "lookup").put("phone_number", e164PhoneNumber)
      )
      val user = response.optJSONObject("user")
      if (response.optBoolean("registered") && user != null) {
        RegistrationStatus.Registered(e164PhoneNumber, user.optString("display_name").takeIf { it.isNotBlank() })
      } else {
        RegistrationStatus.NotRegistered(e164PhoneNumber)
      }
    } catch (error: Exception) {
      if (error.message.orEmpty().contains("not registered", ignoreCase = true)) {
        RegistrationStatus.NotRegistered(e164PhoneNumber)
      } else {
        RegistrationStatus.BackendUnavailable(
          e164PhoneNumber,
          error.message ?: "Unable to check this number right now."
        )
      }
    }
  }
}
