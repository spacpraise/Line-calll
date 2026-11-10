package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.model.CountryList
import com.example.ui.onboarding.EmailEntryScreen
import com.example.ui.onboarding.LoginScreen
import com.example.ui.onboarding.PhoneNumberSetupScreen
import com.example.ui.theme.LineCallTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthFlowUiTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun emailEntryKeepsEachTypedCharacterOnce() {
    var email = ""
    composeRule.setContent {
      LineCallTheme {
        EmailEntryScreen(
          email = email,
          error = null,
          onEmailChanged = { email = it },
          onContinue = {},
          onBack = {}
        )
      }
    }
    composeRule.onNodeWithTag("email_input").performTextInput("sparkpraisesamuel@gmail.com")
    composeRule.runOnIdle {
      assertTrue(email == "sparkpraisesamuel@gmail.com")
    }
  }

  @Test
  fun loginBackButtonReturnsToEmailEntry() {
    var wentBack = false
    composeRule.setContent {
      LineCallTheme {
        LoginScreen(
          identifier = "samuel@example.com",
          displayName = "Samuel",
          avatarUrl = null,
          password = "",
          error = null,
          busy = false,
          onPasswordChanged = {},
          onLogin = {},
          onSwitchAccount = { wentBack = true },
          onForgotPassword = {}
        )
      }
    }
    composeRule.onNodeWithTag("login_switch_back").performClick()
    composeRule.runOnIdle {
      assertTrue(wentBack)
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
