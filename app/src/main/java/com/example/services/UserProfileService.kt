package com.example.services

import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class RegistrationStatus {
  data object NotChecked : RegistrationStatus()
  data class Registered(val e164: String, val name: String?) : RegistrationStatus()
  data class NotRegistered(val e164: String) : RegistrationStatus()
  data class BackendUnavailable(val e164: String, val message: String) : RegistrationStatus()
}

class UserProfileService(private val authService: LineCallAuthService) {
  private val _currentUser = MutableStateFlow<UserAccount?>(null)
  val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

  fun setUser(user: UserAccount) {
    _currentUser.value = user
    authService.updateCurrentUser(user)
  }

  /**
   * Formats national number with dial code to E.164 standard.
   * Strips all non-digit characters except leading '+'.
   */
  fun formatToE164(dialCode: String, nationalNumber: String): String {
    val cleanDialCode = if (dialCode.startsWith("+")) dialCode else "+$dialCode"
    val digitsOnly = nationalNumber.replace(Regex("[^0-9]"), "").trimStart('0')
    return "$cleanDialCode$digitsOnly"
  }

  fun validateE164(e164: String): Boolean {
    // E.164 format: + followed by 7 to 15 digits
    return e164.matches(Regex("^\\+[1-9][0-9]{6,14}$"))
  }

  /**
   * Registers user's phone number as unverified.
   * Phone-number ownership verification is kept as a separate future capability.
   */
  fun registerPhoneNumber(e164PhoneNumber: String): Result<UserAccount> {
    if (!validateE164(e164PhoneNumber)) {
      return Result.failure(IllegalArgumentException("Please enter a valid international phone number in E.164 format (e.g. +14155552671)."))
    }

    val current = _currentUser.value ?: UserAccount(
      id = "temp_${System.currentTimeMillis()}",
      email = "user@linecall.app",
      phoneNumber = e164PhoneNumber,
      isPhoneVerified = false,
      displayName = "Line Call User"
    )

    val updated = current.copy(
      phoneNumber = e164PhoneNumber,
      isPhoneVerified = false // Explicitly stored as unverified
    )
    _currentUser.value = updated
    authService.updateCurrentUser(updated)

    // When Supabase is configured:
    // TODO: supabaseClient.from("users").upsert(mapOf("phone_number" to e164PhoneNumber, "phone_verified" to false))
    return Result.success(updated)
  }

  fun updateProfile(displayName: String, avatarUrl: String?): UserAccount? {
    val current = _currentUser.value ?: return null
    val updated = current.copy(
      displayName = displayName.ifBlank { current.displayName },
      avatarUrl = avatarUrl ?: current.avatarUrl
    )
    _currentUser.value = updated
    authService.updateCurrentUser(updated)
    return updated
  }

  /**
   * Checks whether the target phone number belongs to a registered Line Call user.
   * In production, this queries Supabase: `SELECT id FROM users WHERE phone_number = :e164`.
   */
  suspend fun checkTargetUserRegistration(e164PhoneNumber: String): RegistrationStatus {
    if (!validateE164(e164PhoneNumber)) {
      return RegistrationStatus.NotRegistered(e164PhoneNumber)
    }

    if (!BackendConfig.isSupabaseConfigured) {
      return RegistrationStatus.BackendUnavailable(
        e164 = e164PhoneNumber,
        message = "Live Supabase backend is not yet connected. Configure SUPABASE_URL in build settings to enable real-time user lookup."
      )
    }

    // TODO: val result = supabaseClient.from("users").select().eq("phone_number", e164PhoneNumber).single()
    return RegistrationStatus.NotRegistered(e164PhoneNumber)
  }
}
