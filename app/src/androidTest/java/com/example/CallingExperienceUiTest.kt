package com.example

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.model.ActiveCallSession
import com.example.data.model.CallSessionState
import com.example.services.CallNetworkSnapshot
import com.example.ui.call.ActiveCallScreen
import com.example.ui.call.IncomingCallScreen
import com.example.ui.theme.LineCallTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallingExperienceUiTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun incomingCallActionsAreAccessibleByTap() {
    var accepted = false
    composeRule.setContent {
      LineCallTheme {
        IncomingCallScreen(
          session = ActiveCallSession(
            callId = "incoming-test",
            contactName = "Alex Morgan",
            phoneNumber = "+2348012345678",
            state = CallSessionState.RINGING,
            isIncoming = true
          ),
          onAccept = { accepted = true },
          onDecline = {}
        )
      }
    }

    composeRule.onNodeWithTag("accept_call_button").performClick()
    composeRule.runOnIdle { assertTrue(accepted) }
  }

  @Test
  fun activeCallShowsNetworkDiagnosticsOnDemand() {
    composeRule.setContent {
      LineCallTheme {
        ActiveCallScreen(
          session = ActiveCallSession(
            callId = "connected-test",
            contactName = "Alex Morgan",
            phoneNumber = "+2348012345678",
            state = CallSessionState.CONNECTED,
            durationSeconds = 42
          ),
          onMuteToggle = {},
          onSpeakerToggle = {},
          onEndCall = {},
          networkSnapshot = CallNetworkSnapshot(
            transport = "Wi-Fi",
            networkAvailable = true,
            connectionQuality = "GOOD",
            roundTripTimeMs = 48.0,
            jitterMs = 7.5,
            packetLossPercent = 0.5,
            audioUploadKbps = 28.0,
            audioDownloadKbps = 31.0
          )
        )
      }
    }

    composeRule.onNodeWithTag("call_network_status").assertExists().performClick()
    composeRule.onNodeWithText("Connection details").assertExists()
    composeRule.onNodeWithText("Round-trip latency").assertExists()
    composeRule.onNodeWithText("Packet loss").assertExists()
  }
}
