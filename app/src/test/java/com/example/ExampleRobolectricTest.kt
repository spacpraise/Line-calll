package com.example

import com.example.services.PhoneNumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberFormatterTest {
  @Test
  fun formatsNationalNumberAsE164() {
    assertEquals("+14155550198", PhoneNumberFormatter.formatToE164("+1", "(415) 555-0198"))
  }

  @Test
  fun removesNationalTrunkZero() {
    assertEquals("+2348031234567", PhoneNumberFormatter.formatToE164("+234", "08031234567"))
  }

  @Test
  fun preservesAlreadyInternationalNumber() {
    assertEquals("+2348031234567", PhoneNumberFormatter.formatToE164("+234", "+234 803 123 4567"))
  }

  @Test
  fun validatesE164LengthAndPrefix() {
    assertTrue(PhoneNumberFormatter.isValidE164("+2348031234567"))
    assertFalse(PhoneNumberFormatter.isValidE164("08031234567"))
    assertFalse(PhoneNumberFormatter.isValidE164("+0123456789"))
    assertFalse(PhoneNumberFormatter.isValidE164("+123"))
  }
}