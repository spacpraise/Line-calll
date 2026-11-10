package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.model.CountryList
import com.example.ui.onboarding.LoginScreen
import com.example.ui.onboarding.PhoneNumberSetupScreen
import com.example.ui.theme.LineCallTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthFlowUiTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun loginBackButtonReturnsToWelcomeInsteadOfRegistration() {
    var wentBack = false
    var openedRegistration = false
    composeRule.setContent {
      LineCallTheme {
        LoginScreen(
          identifier = "",
          password = "",
          error = null,
          busy = false,
          onIdentifierChanged = {},
          onPasswordChanged = {},
          onLogin = {},
          onBack = { wentBack = true },
          onRegister = { openedRegistration = true },
          onForgotPassword = {}
        )
      }
    }
    composeRule.onNodeWithTag("login_back_button").performClick()
    composeRule.runOnIdle {
      assertTrue(wentBack)
      assertFalse(openedRegistration)
    }
  }

  @Test
  fun existingEmailErrorOffersDirectLoginAction() {
    var loginSelected = false
    composeRule.setContent {
      LineCallTheme {
        PhoneNumberSetupScreen(
          selectedCountry = CountryList.defaultCountry,
          phoneNumber = "",
          error = "An account with this email already exists. Please log in.",
          onCountrySelected = {},
          onPhoneNumberChanged = {},
          onSubmit = {},
          onBack = {},
          onLoginInstead = { loginSelected = true }
        )
      }
    }
    composeRule.onNodeWithTag("existing_account_login_button").performClick()
    composeRule.runOnIdle { assertTrue(loginSelected) }
  }
}
