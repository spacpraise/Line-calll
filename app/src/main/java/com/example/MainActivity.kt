package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.example.ui.onboarding.EmailEntryScreen
import com.example.ui.onboarding.PasswordSetupScreen
import com.example.ui.onboarding.ProfileSetupScreen
import com.example.ui.onboarding.RegistrationScreen
import com.example.ui.onboarding.WelcomeScreen
import com.example.ui.onboarding.LoginScreen
import com.example.ui.onboarding.ForgotPasswordScreen
import com.example.ui.onboarding.NewPasswordScreen
import com.example.ui.recents.RecentsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.LineCallTheme
import com.example.ui.theme.OneUiDarkBackground
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
  var pendingIncomingCallIntent by mutableStateOf<Intent?>(null)
    private set

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pendingIncomingCallIntent = intent.takeIf { it.hasExtra("linecall_call_id") }
    if (pendingIncomingCallIntent != null) setCallWindowMode(showOnLockScreen = true, wakeScreen = true)
    enableEdgeToEdge()
    setContent { LineCallTheme { LineCallApp() } }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    pendingIncomingCallIntent = intent.takeIf { it.hasExtra("linecall_call_id") }
    if (pendingIncomingCallIntent != null) setCallWindowMode(showOnLockScreen = true, wakeScreen = true)
  }

  fun setCallWindowMode(showOnLockScreen: Boolean, wakeScreen: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(showOnLockScreen)
      setTurnScreenOn(wakeScreen)
    } else {
      if (showOnLockScreen) window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
      else window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
      if (wakeScreen) window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
      else window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }
  }

  fun clearPendingIncomingCallIntent() {
    pendingIncomingCallIntent = null
  }
}

@Composable
fun LineCallApp(viewModel: LineCallViewModel = viewModel()) {
  val context = LocalContext.current
  val hostActivity = context as? MainActivity
  val pendingCallIntent = hostActivity?.pendingIncomingCallIntent
  val focusManager = LocalFocusManager.current
  val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
  val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
  val enteredNumber by viewModel.enteredNumber.collectAsStateWithLifecycle()
  val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
  val emailInput by viewModel.emailInput.collectAsStateWithLifecycle()
  val phoneInput by viewModel.phoneInput.collectAsStateWithLifecycle()
  val displayNameInput by viewModel.displayNameInput.collectAsStateWithLifecycle()
  val loginIdentifier by viewModel.loginIdentifier.collectAsStateWithLifecycle()
  val passwordInput by viewModel.passwordInput.collectAsStateWithLifecycle()
  val confirmPasswordInput by viewModel.confirmPasswordInput.collectAsStateWithLifecycle()
  val isAuthBusy by viewModel.isAuthBusy.collectAsStateWithLifecycle()
  val authError by viewModel.authError.collectAsStateWithLifecycle()
  val recoveryEmail by viewModel.recoveryEmail.collectAsStateWithLifecycle()
  val recoveryPhone by viewModel.recoveryPhone.collectAsStateWithLifecycle()
  val newPassword by viewModel.newPassword.collectAsStateWithLifecycle()
  val confirmNewPassword by viewModel.confirmNewPassword.collectAsStateWithLifecycle()

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

  // Proactively dismiss any dangling FloatingActionMode (text selection toolbar) on transition
  LaunchedEffect(currentScreen, selectedTab, selectedContactDetail, showAddContactScreen) {
    focusManager.clearFocus(force = true)
  }

  var pendingMicrophoneAction by remember { mutableStateOf<(() -> Unit)?>(null) }

  LaunchedEffect(pendingCallIntent) {
    if (pendingCallIntent != null) {
      val callId = pendingCallIntent.getStringExtra("linecall_call_id")
      val action = pendingCallIntent.getStringExtra("linecall_call_action")
      if (!callId.isNullOrBlank() && !action.isNullOrBlank()) {
        viewModel.handleIncomingCallAction(action, callId)
        // Keep the lock-screen call window active until polling restores this exact call.
        withTimeoutOrNull(20_000L) {
          viewModel.activeCall.first { it?.callId == callId }
        }
      }
      hostActivity?.clearPendingIncomingCallIntent()
    }
  }

  LaunchedEffect(activeCall, pendingCallIntent) {
    val incomingSession = activeCall?.takeIf { it.isIncoming }
    when {
      incomingSession != null ->
        hostActivity?.setCallWindowMode(
          showOnLockScreen = true,
          wakeScreen = incomingSession.state == CallSessionState.RINGING
        )
      pendingCallIntent == null ->
        hostActivity?.setCallWindowMode(showOnLockScreen = false, wakeScreen = false)
    }
  }

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

  // Predictive Back handling
  BackHandler(enabled = activeCall != null) {
    focusManager.clearFocus(force = true)
    viewModel.endCall()
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail != null) {
    focusManager.clearFocus(force = true)
    viewModel.selectContactDetail(null)
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && showAddContactScreen) {
    focusManager.clearFocus(force = true)
    viewModel.setShowAddContactScreen(false)
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && !showAddContactScreen && currentScreen == ScreenState.SETTINGS) {
    focusManager.clearFocus(force = true)
    viewModel.navigateTo(ScreenState.MAIN_APP)
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && !showAddContactScreen && currentScreen == ScreenState.PROFILE_SETUP) {
    focusManager.clearFocus(force = true)
    viewModel.navigateTo(ScreenState.MAIN_APP)
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && !showAddContactScreen && currentScreen == ScreenState.PHONE_SETUP) {
    focusManager.clearFocus(force = true)
    viewModel.backFromPhoneSetup()
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && !showAddContactScreen && currentScreen == ScreenState.PASSWORD_SETUP) {
    focusManager.clearFocus(force = true)
    viewModel.navigateTo(ScreenState.EMAIL_REGISTRATION)
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && !showAddContactScreen && currentScreen == ScreenState.EMAIL_REGISTRATION) {
    focusManager.clearFocus(force = true)
    viewModel.navigateTo(ScreenState.WELCOME)
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && !showAddContactScreen && currentScreen == ScreenState.LOGIN) {
    focusManager.clearFocus(force = true)
    viewModel.navigateTo(ScreenState.WELCOME)
  }

  BackHandler(enabled = activeCall == null && currentScreen == ScreenState.RESET_PASSWORD) {
    focusManager.clearFocus(force = true)
    viewModel.navigateTo(ScreenState.FORGOT_PASSWORD)
  }

  BackHandler(enabled = activeCall == null && currentScreen == ScreenState.FORGOT_PASSWORD) {
    focusManager.clearFocus(force = true)
    viewModel.navigateTo(ScreenState.LOGIN)
  }

  BackHandler(enabled = activeCall == null && selectedContactDetail == null && !showAddContactScreen && currentScreen == ScreenState.MAIN_APP && selectedTab != MainTab.KEYPAD) {
    focusManager.clearFocus(force = true)
    viewModel.selectTab(MainTab.KEYPAD)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(OneUiDarkBackground)
  ) {
    when (currentScreen) {
      ScreenState.BOOTING -> {
        androidx.compose.foundation.layout.Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
          androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_launcher_linecall_1794083310223),
            contentDescription = "Line Call",
            modifier = Modifier.size(104.dp)
          )
          androidx.compose.material3.Text(
            text = "Line Call",
            color = androidx.compose.ui.graphics.Color.White,
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 18.dp, bottom = 18.dp)
          )
          androidx.compose.material3.CircularProgressIndicator(
            color = com.example.ui.theme.CallGreen,
            strokeWidth = 2.dp,
            modifier = Modifier.size(26.dp)
          )
        }
      }
      ScreenState.WELCOME -> {
        WelcomeScreen(
          onGetStarted = { viewModel.navigateTo(ScreenState.EMAIL_REGISTRATION) },
          onLogIn = { viewModel.navigateTo(ScreenState.LOGIN) }
        )
      }

      ScreenState.EMAIL_REGISTRATION -> {
        EmailEntryScreen(
          email = emailInput,
          error = authError,
          onEmailChanged = { viewModel.updateEmailInput(it) },
          onContinue = { viewModel.continueToPasswordSetup() },
          onBack = { viewModel.navigateTo(ScreenState.WELCOME) }
        )
      }

      ScreenState.PASSWORD_SETUP -> {
        PasswordSetupScreen(
          email = emailInput,
          password = passwordInput,
          confirmPassword = confirmPasswordInput,
          error = authError,
          busy = isAuthBusy,
          onPasswordChanged = { viewModel.updatePasswordInput(it) },
          onConfirmPasswordChanged = { viewModel.updateConfirmPasswordInput(it) },
          onCreateAccount = { viewModel.registerAccount() },
          onBack = { viewModel.navigateTo(ScreenState.EMAIL_REGISTRATION) }
        )
      }

      ScreenState.LOGIN -> {
        LoginScreen(
          identifier = loginIdentifier,
          password = passwordInput,
          error = authError,
          busy = isAuthBusy,
          onIdentifierChanged = { viewModel.updateLoginIdentifier(it) },
          onPasswordChanged = { viewModel.updatePasswordInput(it) },
          onLogin = { viewModel.signIn() },
          onRegister = { viewModel.navigateTo(ScreenState.EMAIL_REGISTRATION) },
          onForgotPassword = { viewModel.navigateTo(ScreenState.FORGOT_PASSWORD) }
        )
      }

      ScreenState.FORGOT_PASSWORD -> {
        ForgotPasswordScreen(
          email = recoveryEmail,
          phoneNumber = recoveryPhone,
          error = authError,
          busy = isAuthBusy,
          onEmailChanged = { viewModel.updateRecoveryEmail(it) },
          onPhoneChanged = { viewModel.updateRecoveryPhone(it) },
          onSubmit = { viewModel.requestPasswordRecovery() },
          onBack = { viewModel.navigateTo(ScreenState.LOGIN) }
        )
      }

      ScreenState.RESET_PASSWORD -> {
        NewPasswordScreen(
          password = newPassword,
          confirmPassword = confirmNewPassword,
          error = authError,
          busy = isAuthBusy,
          onPasswordChanged = { viewModel.updateNewPassword(it) },
          onConfirmPasswordChanged = { viewModel.updateConfirmNewPassword(it) },
          onSubmit = { viewModel.completePasswordRecovery() }
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
          onBack = { viewModel.backFromPhoneSetup() }
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
                  callHistory = callHistory,
                  onNumberChanged = { viewModel.setEnteredNumber(it) },
                  onCall = { runWithMicrophonePermission { viewModel.onCallButtonPressed() } },
                  onSelectSuggestion = { runWithMicrophonePermission { viewModel.callRecent(it) } },
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

    // Animate the hand-off from the incoming-call surface into the live call.
    activeCall?.let { session ->
      androidx.compose.animation.AnimatedContent(
        targetState = session.state == CallSessionState.CONNECTED,
        transitionSpec = {
          (androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(420)
          ) + androidx.compose.animation.scaleIn(
            initialScale = 0.94f,
            animationSpec = androidx.compose.animation.core.spring(
              dampingRatio = 0.82f,
              stiffness = 260f
            )
          )) togetherWith androidx.compose.animation.fadeOut(
            animationSpec = androidx.compose.animation.core.tween(160)
          )
        },
        label = "incoming_to_active_call"
      ) { connected ->
        if (!connected && session.isIncoming && session.state == CallSessionState.RINGING) {
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
}
