package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Line Call", appName)
  }

  @Test
  fun `verify e164 formatting`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val supabase = com.example.services.LineCallSupabaseClient(context)
    val authService = com.example.services.LineCallAuthService(supabase)
    val profileService = com.example.services.UserProfileService(authService, supabase)
    val formatted = profileService.formatToE164("+1", "(415) 555-0198")
    assertEquals("+14155550198", formatted)
  }
}
