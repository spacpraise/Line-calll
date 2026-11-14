package com.example

import android.Manifest
import android.app.NotificationManager
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.CallAudioRoute
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
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LineCallTheme
import com.example.services.IncomingCallLifecycleService
import com.example.update.LineCallUpdateManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
  var pendingIncomingCallIntent by mutableStateOf<Intent?>(null)
    private set
  var resumeCount by mutableStateOf(0)
    private set
  var themeMode by mutableStateOf(AppThemeMode.SYSTEM)
    private set

  fun updateThemeMode(mode: AppThemeMode) {
    themeMode = mode
    getSharedPreferences("line_call_preferences", android.content.Context.MODE_PRIVATE)
      .edit().putString("theme_mode", mode.name).apply()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    IncomingCallLifecycleService.recoverIfNeeded(this)
    pendingIncomingCallIntent = intent.takeIf { it.hasExtra("linecall_call_id") }
    if (pendingIncomingCallIntent != null) {

      setCallWindowMode(showOnLockScreen = true, wakeScreen = true)
    }
    themeMode = runCatching {
      AppThemeMode.valueOf(
        getSharedPreferences("line_call_preferences", android.content.Context.MODE_PRIVATE)
          .getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
      )
    }.getOrDefault(AppThemeMode.SYSTEM)
    enableEdgeToEdge()
    setContent {
      LineCallTheme(mode = themeMode) {
        LineCallApp(themeMode = themeMode, onThemeModeChange = ::updateThemeMode)
      }
    }
    // WorkManager initialization is deferred until after the first frame so it
    // cannot hold up the launch animation or first screen draw.
    window.decorView.post {
      LineCallUpdateManager.reconcileInstalledVersion(this)
      LineCallUpdateManager.schedule(this)
    }
    if (intent.getBooleanExtra("linecall_install_update", false)) {
      window.decorView.post { LineCallUpdateManager.installPending(this) }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    pendingIncomingCallIntent = intent.takeIf { it.hasExtra("linecall_call_id") }
    if (pendingIncomingCallIntent != null) {

      setCallWindowMode(showOnLockScreen = true, wakeScreen = true)
    }
    if (intent.getBooleanExtra("linecall_install_update", false)) {
      window.decorView.post { LineCallUpdateManager.installPending(this) }
    }
  }

  override fun onResume() {
    super.onResume()
    resumeCount += 1
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
fun LineCallApp(
  themeMode: AppThemeMode,
  onThemeModeChange: (AppThemeMode) -> Unit,
  viewModel: LineCallViewModel = viewModel()
) {
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
  val loginDisplayName by viewModel.loginDisplayName.collectAsStateWithLifecycle()
  val loginAvatarUrl by viewModel.loginAvatarUrl.collectAsStateWithLifecycle()
  val passwordInput by viewModel.passwordInput.collectAsStateWithLifecycle()
  val confirmPasswordInput by viewModel.confirmPasswordInput.collectAsStateWithLifecycle()
  val isAuthBusy by viewModel.isAuthBusy.collectAsStateWithLifecycle()
  val authError by viewModel.authError.collectAsStateWithLifecycle()
  val authNotice by viewModel.authNotice.collectAsStateWithLifecycle()
  val recoveryEmail by viewModel.recoveryEmail.collectAsStateWithLifecycle()
  val recoveryPhone by viewModel.recoveryPhone.collectAsStateWithLifecycle()
  val newPassword by viewModel.newPassword.collectAsStateWithLifecycle()
  val confirmNewPassword by viewModel.confirmNewPassword.collectAsStateWithLifecycle()

  val contacts by viewModel.contacts.collectAsStateWithLifecycle()
  val contactSearchQuery by viewModel.contactSearchQuery.collectAsStateWithLifecycle()
  val callHistory by viewModel.callHistory.collectAsStateWithLifecycle()
  val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
  val audioRoutes by viewModel.audioRoutes.collectAsStateWithLifecycle()
  val selectedAudioRouteId by viewModel.selectedAudioRouteId.collectAsStateWithLifecycle()
  val networkSnapshot by viewModel.networkSnapshot.collectAsStateWithLifecycle()
  val backendNotice by viewModel.backendNotice.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val contactSyncStatus by viewModel.contactSyncStatus.collectAsStateWithLifecycle()

  val selectedContactDetail by viewModel.selectedContactDetail.collectAsStateWithLifecycle()
  val showAddContactScreen by viewModel.showAddContactScreen.collectAsStateWithLifecycle()
  val missedCallBannerDismissed by viewModel.missedCallBannerDismissed.collectAsStateWithLifecycle()
  val recentsFilterType by viewModel.recentsFilterType.collectAsStateWithLifecycle()

  // Proactively dismiss any dangling FloatingActionMode (text selection toolbar) on transition
  LaunchedEffect(currentScreen, selectedTab, selectedContactDetail, showAddContactScreen) {
    focusManager.clearFocus(force = true)
  }

  var pendingMicrophoneAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var showUpdatePrompt by remember { mutableStateOf(false) }
  var showFullScreenPermissionPrompt by remember { mutableStateOf(false) }

  LaunchedEffect(pendingCallIntent) {
    if (pendingCallIntent != null) {
      val callId = pendingCallIntent.getStringExtra("linecall_call_id")
      val action = pendingCallIntent.getStringExtra("linecall_call_action")
      if (!callId.isNullOrBlank() && !action.isNullOrBlank()) {
        viewModel.handleIncomingCallAction(
          action,
          callId,
          pendingCallIntent.getStringExtra("linecall_caller_name"),
          pendingCallIntent.getStringExtra("linecall_caller_phone").orEmpty()
        )
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

  val contactsPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) viewModel.importDeviceContacts()
    else Toast.makeText(context, "Contact access was not granted. You can still add or dial numbers manually.", Toast.LENGTH_LONG).show()
  }

  val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    viewModel.refreshAudioRoutes()
    if (!granted) {
      Toast.makeText(context, "You can still use available call audio routes. Android may show a generic Bluetooth name.", Toast.LENGTH_LONG).show()
    }
  }

  LaunchedEffect(currentUser?.id) {
    if (currentUser != null) {
      val preferences = context.getSharedPreferences("line_call_preferences", android.content.Context.MODE_PRIVATE)
      if (!preferences.getBoolean("contact_import_prompted", false)) {
        preferences.edit().putBoolean("contact_import_prompted", true).apply()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
          viewModel.importDeviceContacts()
        } else {
          contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
      }
    }
  }

  fun requestContactImport() {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
      viewModel.importDeviceContacts()
    } else {
      contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
  }

  fun runWithMicrophonePermission(action: () -> Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
      action()
    } else {
      pendingMicrophoneAction = action
      microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  val appResumeCount = hostActivity?.resumeCount ?: 0
  LaunchedEffect(appResumeCount, currentUser?.id) {
    showUpdatePrompt = LineCallUpdateManager.hasPendingUpdate(context)
    val permissionPreferences = context.getSharedPreferences("line_call_preferences", android.content.Context.MODE_PRIVATE)
    showFullScreenPermissionPrompt = currentUser != null && Build.VERSION.SDK_INT >= 34 &&
      !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent() &&
      !permissionPreferences.getBoolean("full_screen_permission_prompted", false)
    viewModel.refreshProfileAfterAppResume()
  }

  LaunchedEffect(currentUser?.id) {
    if (currentUser != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val permissionPreferences = context.getSharedPreferences("line_call_preferences", android.content.Context.MODE_PRIVATE)
      if (!permissionPreferences.getBoolean("notification_permission_prompted", false)) {
        permissionPreferences.edit().putBoolean("notification_permission_prompted", true).apply()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
          notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
      }
    }
  }

  // Predictive Back handling
  BackHandler(enabled = activeCall != null) {
    focusManager.clearFocus(force = true)
    if (activeCall?.state == CallSessionState.ENDED) viewModel.dismissEndedCall()
    else viewModel.endCall()
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
    viewModel.navigateTo(ScreenState.EMAIL_REGISTRATION)
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
      .background(MaterialTheme.colorScheme.background)
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
            color = MaterialTheme.colorScheme.onBackground,
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
          onGetStarted = { viewModel.navigateTo(ScreenState.EMAIL_REGISTRATION) }
        )
      }

      ScreenState.EMAIL_REGISTRATION -> {
        EmailEntryScreen(
          email = emailInput,
          error = authError,
          onEmailChanged = { viewModel.updateEmailInput(it) },
          onContinue = { viewModel.checkEmailAndContinue() },
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
          displayName = loginDisplayName,
          avatarUrl = loginAvatarUrl,
          password = passwordInput,
          error = authError,
          busy = isAuthBusy,
          onPasswordChanged = { viewModel.updatePasswordInput(it) },
          onLogin = { viewModel.signIn() },
          onSwitchAccount = {
            viewModel.updatePasswordInput("")
            viewModel.navigateTo(ScreenState.EMAIL_REGISTRATION)
          },
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
          onBack = { viewModel.backFromPhoneSetup() },
          onLoginInstead = { viewModel.goToLoginFromRegistration() }
        )
      }

      ScreenState.PROFILE_SETUP -> {
        ProfileSetupScreen(
          displayName = displayNameInput,
          error = authError,
          onDisplayNameChanged = { viewModel.updateDisplayNameInput(it) },
          onComplete = { viewModel.completeProfileSetup() },
          onSkip = { viewModel.skipProfileSetup() }
        )
      }

      ScreenState.SETTINGS -> {
        SettingsScreen(
          currentUser = currentUser,
          onUpdateDisplayName = { viewModel.saveDisplayName(it) },
          onUpdatePhoneNumber = { viewModel.updateProfilePhoneNumber(it) },
          onUpdateEmail = { viewModel.updateProfileEmail(it) },
          onAvatarSelected = { viewModel.updateProfileAvatar(it) },
          onSignOut = { viewModel.signOut() },
          onBack = { viewModel.navigateTo(ScreenState.MAIN_APP) },
          themeMode = themeMode,
          onThemeModeChange = onThemeModeChange,
          onManagePermissions = {
            runCatching {
              context.startActivity(Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:${context.packageName}")
              ))
            }
          }
        )
      }

      ScreenState.MAIN_APP -> {
        Scaffold(
          containerColor = MaterialTheme.colorScheme.background,
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
            androidx.compose.animation.AnimatedContent(
              targetState = selectedTab,
              transitionSpec = {
                (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) +
                  androidx.compose.animation.slideInHorizontally(
                    animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    initialOffsetX = { width -> width / 18 }
                  )) togetherWith
                  (androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(110)) +
                    androidx.compose.animation.slideOutHorizontally(
                      animationSpec = androidx.compose.animation.core.tween(160),
                      targetOffsetX = { width -> -width / 24 }
                    ))
              },
              label = "main_tab_transition"
            ) { tab ->
              when (tab) {
              MainTab.KEYPAD -> {
                KeypadScreen(
                  modifier = Modifier.fillMaxSize().padding(bottom = 112.dp),
                  enteredNumber = enteredNumber,
                  selectedCountry = selectedCountry,
                  callHistory = callHistory,
                  contacts = contacts,
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
                  onProfileClick = { viewModel.navigateTo(ScreenState.SETTINGS) },
                  onImportContacts = { requestContactImport() },
                  syncStatus = contactSyncStatus
                )
              }
              }
            }

            // Floating Capsule Bottom Navigation Bar (Image 1, 2, 4, 6)
            if (!(selectedTab == MainTab.KEYPAD && enteredNumber.isNotBlank())) {
              OneUiFloatingBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
              )
            }
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

    if (showFullScreenPermissionPrompt) {
      AlertDialog(
        onDismissRequest = {\n          context.getSharedPreferences("line_call_preferences", android.content.Context.MODE_PRIVATE).edit()\n            .putBoolean("full_screen_permission_prompted", true).apply()\n          showFullScreenPermissionPrompt = false\n        },
        title = { Text("Allow incoming calls over the lock screen") },
        text = { Text("Android currently blocks Line Call from opening its incoming-call screen over your locked phone. Allow full-screen notifications so incoming calls can wake the screen and show Answer and Decline.") },
        confirmButton = {
          Button(onClick = {
            showFullScreenPermissionPrompt = false
            if (Build.VERSION.SDK_INT >= 34) {
              runCatching {
                context.startActivity(Intent(
                  android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                  android.net.Uri.parse("package:${context.packageName}")
                ))
              }
            }
          }) { Text("Open Android setting") }
        },
        dismissButton = {
          TextButton(onClick = {\n            context.getSharedPreferences("line_call_preferences", android.content.Context.MODE_PRIVATE).edit()\n              .putBoolean("full_screen_permission_prompted", true).apply()\n            showFullScreenPermissionPrompt = false\n          }) { Text("Not now") }
        }
      )
    }

    if (showUpdatePrompt && LineCallUpdateManager.hasPendingUpdate(context)) {
      AlertDialog(
        onDismissRequest = { showUpdatePrompt = false },
        title = { Text("Downloaded Line Call update ready") },
        text = {
          androidx.compose.foundation.layout.Column {
            Text("Version ${LineCallUpdateManager.pendingVersion(context)}")
            Text(
              LineCallUpdateManager.pendingNotes(context).trim().take(900)
                .ifBlank { "Bug fixes and improvements are ready to install." },
              modifier = Modifier.padding(top = 10.dp)
            )
            Text(
              "This update is already downloaded and saved on this phone. Installing it will use the saved APK; it will not download it again.",
              modifier = Modifier.padding(top = 10.dp)
            )
          }
        },
        confirmButton = {
          Button(onClick = {
            val launched = LineCallUpdateManager.installPending(context)
            showUpdatePrompt = !launched
            if (!launched) {
              Toast.makeText(context, "The saved update could not be opened. It is still available to try again.", Toast.LENGTH_LONG).show()
            }
          }) { Text("Install downloaded update") }
        },
        dismissButton = {
          TextButton(onClick = { showUpdatePrompt = false }) { Text("Not now") }
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
            audioRoutes = audioRoutes,
            selectedAudioRouteId = selectedAudioRouteId,
            networkSnapshot = networkSnapshot,
            onRefreshAudioRoutes = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
              ) {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
              } else {
                viewModel.refreshAudioRoutes()
              }
            },
            onAudioRouteSelected = { viewModel.selectAudioRoute(it) },
            onEndCall = { viewModel.endCall() },
            onRecall = { viewModel.recallCall(session) }
          )
        }
      }
    }
  }
}
