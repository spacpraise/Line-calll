package com.example.services

/**
 * Pure phone-number formatting and validation helpers.
 * Kept free of Android dependencies so unit tests do not need Robolectric or network downloads.
 */
object PhoneNumberFormatter {
  fun formatToE164(dialCode: String, nationalNumber: String): String {
    val raw = nationalNumber.trim()
    if (raw.startsWith("+")) return raw.replace(Regex("[^0-9+]"), "")
    val cleanDialCode = if (dialCode.startsWith("+")) dialCode else "+$dialCode"
    val digitsOnly = raw.replace(Regex("[^0-9]"), "").trimStart('0')
    return "$cleanDialCode$digitsOnly"
  }

  fun isValidE164(e164: String): Boolean =
    e164.matches(Regex("^\\+[1-9][0-9]{6,14}$"))
}