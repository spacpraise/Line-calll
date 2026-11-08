package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.CallSessionState
import com.example.ui.LineCallViewModel
import com.example.ui.MainTab
import com.example.ui.ScreenState
import com.example.ui.call.ActiveCallScreen
import com.example.ui.call.IncomingCallScreen
import com.example.ui.common.BackendNoticeDialog
import com.example.ui.common.OneUiFloatingBottomNav
import com.example.ui.common.OneUiTopBar
import com.example.ui.contacts.AddContactOneUiScreen
import com.example.ui.contacts.ContactDetailScreen
import com.example.ui.contacts.ContactsScreen
import com.example.ui.dialer.KeypadScreen
import com.example.ui.onboarding.PhoneNumberSetupScreen
import com.example.ui.onboarding.ProfileSetupScreen
import com.example.ui.onboarding.RegistrationScreen
import com.example.ui.onboarding.WelcomeScreen
import com.example.ui.recents.RecentsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.LineCallTheme
import com.example.ui.theme.OneUiDarkBackground

class MainActivity : ComponentActivity() {
  private var authCallbackUri by mutableStateOf<Uri?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    authCallbackUri = intent?.data
    enableEdgeToEdge()
    setContent {
      LineCallTheme {
        LineCallApp(
          authCallbackUri = authCallbackUri,
          onAuthCallbackHandled = { authCallbackUri = null }
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    authCallbackUri = intent.data
  }
}

@Composable
fun LineCallApp(
  viewModel: LineCallViewModel = viewModel(),
  authCallbackUri: Uri? = null,
  onAuthCallbackHandled: () -> Unit = {}
) {
  val context = LocalContext.current
  val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
  val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
  val enteredNumber by viewModel.enteredNumber.collectAsStateWithLifecycle()
  val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
  val emailInput by viewModel.emailInput.collectAsStateWithLifecycle()
  val phoneInput by viewModel.phoneInput.collectAsStateWithLifecycle()
  val displayNameInput by viewModel.displayNameInput.collectAsStateWithLifecycle()
  val magicLinkSent by viewModel.magicLinkSent.collectAsStateWithLifecycle()
  val authError by viewModel.authError.collectAsStateWithLifecycle()

  val contacts by viewModel.contacts.collectAsStateWithLifecycle()
  val contactSearchQuery by viewModel.contactSearchQuery.collectAsStateWithLifecycle()
  val callHistory by viewModel.callHistory.collectAsStateWithLifecycle()
  val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
  val backendNotice by viewModel.backendNotice.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

  val selectedContactDetail by viewModel.selectedContactDetail.collectAsStateWithLifecycle()
  val showAddContactScreen by viewModel.showAddContactScreen.collectAsStateWithLifecycle()
  val missedCallBannerDismissed by viewModel.missedCallBannerDismissed.collectAsStateWithLifecycle()
  val recentsFilterType by viewModel.recentsFilterType.collectAsStateWithLifecycle()

  var pendingMicrophoneAction by remember { mutableStateOf<(() -> Unit)?>(null) }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { }

  val microphonePermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) pendingMicrophoneAction?.invoke()
    else Toast.makeText(context, "Microphone access is needed to make voice calls.", Toast.LENGTH_LONG).show()
  }

  fun runWithMicrophonePermission(action: () -> Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
      action()
    } else {
      pendingMicrophoneAction = action
      microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  LaunchedEffect(authCallbackUri) {
    authCallbackUri?.let { uri ->
      viewModel.verifyMagicLinkCallback(uri.toString())
      onAuthCallbackHandled()
    }
  }

  // Predictive Back handling
  BackHandler(enabled = activeCall != null) {
    viewModel.endCall()
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail != null) {
    viewModel.selectContactDetail(null)
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && showAddContactScreen) {
    viewModel.setShowAddContactScreen(false)
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && !showAddContactScreen && currentScreen == ScreenState.SETTINGS) {
    viewModel.navigateTo(ScreenState.MAIN_APP)
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && !showAddContactScreen && currentScreen == ScreenState.PROFILE_SETUP) {
    viewModel.navigateTo(ScreenState.MAIN_APP)
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && !showAddContactScreen && currentScreen == ScreenState.PHONE_SETUP) {
    viewModel.navigateTo(ScreenState.EMAIL_REGISTRATION)
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && !showAddContactScreen && currentScreen == ScreenState.EMAIL_REGISTRATION) {
    viewModel.navigateTo(ScreenState.WELCOME)
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && !showAddContactScreen && currentScreen == ScreenState.MAIN_APP && selectedTab != MainTab.KEYPAD) {
    viewModel.selectTab(MainTab.KEYPAD)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(OneUiDarkBackground)
  ) {
    when (currentScreen) {
      ScreenState.WELCOME -> {
        WelcomeScreen(
          onGetStarted = { viewModel.navigateTo(ScreenState.EMAIL_REGISTRATION) },
          onLogIn = { viewModel.navigateTo(ScreenState.EMAIL_REGISTRATION) }
        )
      }

      ScreenState.EMAIL_REGISTRATION -> {
        RegistrationScreen(
          email = emailInput,
          magicLinkSent = magicLinkSent,
          authError = authError,
          onEmailChanged = { viewModel.updateEmailInput(it) },
          onSubmitEmail = { viewModel.submitEmail() },
          onBack = { viewModel.navigateTo(ScreenState.WELCOME) }
        )
      }

      ScreenState.PHONE_SETUP -> {
        PhoneNumberSetupScreen(
          selectedCountry = selectedCountry,
          phoneNumber = phoneInput,
          error = authError,
          onCountrySelected = { viewModel.selectCountry(it) },
          onPhoneNumberChanged = { viewModel.updatePhoneInput(it) },
          onSubmit = { viewModel.submitPhoneNumber() },
          onBack = { viewModel.navigateTo(ScreenState.EMAIL_REGISTRATION) }
        )
      }

      ScreenState.PROFILE_SETUP -> {
        ProfileSetupScreen(
          displayName = displayNameInput,
          onDisplayNameChanged = { viewModel.updateDisplayNameInput(it) },
          onComplete = { viewModel.completeProfileSetup() },
          onSkip = { viewModel.skipProfileSetup() }
        )
      }

      ScreenState.SETTINGS -> {
        SettingsScreen(
          currentUser = currentUser,
          onUpdateDisplayName = { viewModel.saveDisplayName(it) },
          onSignOut = { viewModel.signOut() },
          onBack = { viewModel.navigateTo(ScreenState.MAIN_APP) }
        )
      }

      ScreenState.MAIN_APP -> {
        Scaffold(
          containerColor = OneUiDarkBackground,
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
          topBar = {
            OneUiTopBar(
              currentTab = selectedTab,
              contactsCount = contacts.size,
              currentUser = currentUser,
              onAddContactClick = { viewModel.setShowAddContactScreen(true) },
              onSearchClick = { viewModel.updateContactSearch("") },
              onFilterClick = { },
              onSettingsClick = { viewModel.navigateTo(ScreenState.SETTINGS) }
            )
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            when (selectedTab) {
              MainTab.KEYPAD -> {
                KeypadScreen(
                  enteredNumber = enteredNumber,
                  selectedCountry = selectedCountry,
                  onAppendChar = { viewModel.appendKeypadChar(it) },
                  onBackspace = { viewModel.backspaceKeypad() },
                  onClear = { viewModel.clearKeypad() },
                  onCall = { runWithMicrophonePermission { viewModel.onCallButtonPressed() } },
                  onSaveContact = { viewModel.setShowAddContactScreen(true) }
                )
              }

              MainTab.RECENTS -> {
                RecentsScreen(
                  callHistory = callHistory,
                  missedCallBannerDismissed = missedCallBannerDismissed,
                  filterType = recentsFilterType,
                  onDismissMissedBanner = { viewModel.dismissMissedCallBanner() },
                  onFilterSelected = { viewModel.setRecentsFilterType(it) },
                  onCallRecord = { runWithMicrophonePermission { viewModel.callRecent(it) } },
                  onClearHistory = { viewModel.clearCallHistory() }
                )
              }

              MainTab.CONTACTS -> {
                ContactsScreen(
                  contacts = contacts,
                  currentUser = currentUser,
                  searchQuery = contactSearchQuery,
                  onSearchQueryChanged = { viewModel.updateContactSearch(it) },
                  onContactClick = { viewModel.selectContactDetail(it) },
                  onProfileClick = { viewModel.navigateTo(ScreenState.SETTINGS) }
                )
              }
            }

            // Floating Capsule Bottom Navigation Bar (Image 1, 2, 4, 6)
            OneUiFloatingBottomNav(
              selectedTab = selectedTab,
              onTabSelected = { viewModel.selectTab(it) },
              modifier = Modifier.align(Alignment.BottomCenter)
            )
          }
        }
      }
    }

    // Contact Details View Overlay (Image 7)
    selectedContactDetail?.let { contact ->
      ContactDetailScreen(
        contact = contact,
        onBack = { viewModel.selectContactDetail(null) },
        onCall = { runWithMicrophonePermission { viewModel.callContact(it) } },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onDelete = { viewModel.deleteContact(it) }
      )
    }

    // Add Contact Screen Overlay (Image 3)
    if (showAddContactScreen) {
      AddContactOneUiScreen(
        initialPhone = enteredNumber,
        currentUser = currentUser,
        onCancel = { viewModel.setShowAddContactScreen(false) },
        onSave = { name, phone ->
          viewModel.addContact(name, phone)
          viewModel.setShowAddContactScreen(false)
        }
      )
    }

    // Backend Notice Dialog
    backendNotice?.let { notice ->
      BackendNoticeDialog(
        notice = notice,
        onDismiss = { viewModel.dismissBackendNotice() }
      )
    }

    // Active or Incoming Voice Call Overlays
    activeCall?.let { session ->
      if (session.isIncoming && session.state == CallSessionState.RINGING) {
        IncomingCallScreen(
          session = session,
          onAccept = { runWithMicrophonePermission { viewModel.acceptIncomingCall() } },
          onDecline = { viewModel.declineIncomingCall() }
        )
      } else {
        ActiveCallScreen(
          session = session,
          onMuteToggle = { viewModel.toggleMute() },
          onSpeakerToggle = { viewModel.toggleSpeaker() },
          onEndCall = { viewModel.endCall() }
        )
      }
    }
  }
}
