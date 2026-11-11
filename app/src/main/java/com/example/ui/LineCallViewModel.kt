package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.data.local.LineCallDatabase
import com.example.data.model.ActiveCallSession
import com.example.data.model.CallRecord
import com.example.data.model.CallType
import com.example.data.model.Contact
import com.example.data.model.Country
import com.example.data.model.CountryList
import com.example.data.model.UserAccount
import com.example.services.CallEvent
import com.example.services.CallInitiationResult
import com.example.services.CallingService
import com.example.services.LineCallAuthService
import com.example.services.LineCallSupabaseClient
import com.example.services.UserProfileService
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers

enum class ScreenState {
  BOOTING,
  WELCOME,
  EMAIL_REGISTRATION,
  PASSWORD_SETUP,
  LOGIN,
  FORGOT_PASSWORD,
  RESET_PASSWORD,
  PHONE_SETUP,
  PROFILE_SETUP,
  MAIN_APP,
  SETTINGS
}

enum class MainTab {
  KEYPAD,
  RECENTS,
  CONTACTS
}

data class BackendNoticeState(
  val title: String,
  val message: String,
  val targetNumber: String,
  val targetName: String?
)

class LineCallViewModel(application: Application) : AndroidViewModel(application) {
  private val database = LineCallDatabase.getDatabase(application)
  private val contactDao = database.contactDao()
  private val callRecordDao = database.callRecordDao()

  private val supabase = LineCallSupabaseClient(application)
  val authService = LineCallAuthService(supabase)
  val userProfileService = UserProfileService(authService, supabase)
  val callingService = CallingService(application, callRecordDao, userProfileService, supabase, viewModelScope)

  private val appLifecycleObserver = LifecycleEventObserver { _, event ->
    when (event) {
      Lifecycle.Event.ON_START -> {
        if (userProfileService.currentUser.value != null) callingService.startIncomingCallPolling()
      }
      Lifecycle.Event.ON_STOP -> callingService.stopIncomingCallPolling()
      else -> Unit
    }
  }

  private val _currentScreen = MutableStateFlow(ScreenState.BOOTING)
  val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

  private val _selectedTab = MutableStateFlow(MainTab.KEYPAD)
  val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

  private val _enteredNumber = MutableStateFlow("")
  val enteredNumber: StateFlow<String> = _enteredNumber.asStateFlow()

  private val _selectedCountry = MutableStateFlow(CountryList.defaultCountry)
  val selectedCountry: StateFlow<Country> = _selectedCountry.asStateFlow()

  private val _emailInput = MutableStateFlow("")
  val emailInput: StateFlow<String> = _emailInput.asStateFlow()

  private val _loginIdentifier = MutableStateFlow("")
  val loginIdentifier: StateFlow<String> = _loginIdentifier.asStateFlow()
  private val _loginDisplayName = MutableStateFlow("")
  val loginDisplayName: StateFlow<String> = _loginDisplayName.asStateFlow()
  private val _loginAvatarUrl = MutableStateFlow<String?>(null)
  val loginAvatarUrl: StateFlow<String?> = _loginAvatarUrl.asStateFlow()

  private val _passwordInput = MutableStateFlow("")
  val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

  private val _confirmPasswordInput = MutableStateFlow("")
  val confirmPasswordInput: StateFlow<String> = _confirmPasswordInput.asStateFlow()

  private val _isAuthBusy = MutableStateFlow(false)
  val isAuthBusy: StateFlow<Boolean> = _isAuthBusy.asStateFlow()

  private val _recoveryEmail = MutableStateFlow("")
  val recoveryEmail: StateFlow<String> = _recoveryEmail.asStateFlow()

  private val _recoveryPhone = MutableStateFlow("")
  val recoveryPhone: StateFlow<String> = _recoveryPhone.asStateFlow()

  private val _recoveryTokenHash = MutableStateFlow("")
  private val _newPassword = MutableStateFlow("")
  val newPassword: StateFlow<String> = _newPassword.asStateFlow()

  private val _confirmNewPassword = MutableStateFlow("")
  val confirmNewPassword: StateFlow<String> = _confirmNewPassword.asStateFlow()

  private var isCompletingRegistration = false
  private var emailChangeAwaitingConfirmation = false
  private var emailBeforeChange = ""

  private val _phoneInput = MutableStateFlow("")
  val phoneInput: StateFlow<String> = _phoneInput.asStateFlow()

  private val _displayNameInput = MutableStateFlow("")
  val displayNameInput: StateFlow<String> = _displayNameInput.asStateFlow()

  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  private val _authNotice = MutableStateFlow<String?>(null)
  val authNotice: StateFlow<String?> = _authNotice.asStateFlow()

  private val _contactSearchQuery = MutableStateFlow("")
  val contactSearchQuery: StateFlow<String> = _contactSearchQuery.asStateFlow()

  // Search filtering runs off the UI thread; unchanged results are not re-emitted.
  val contacts: StateFlow<List<Contact>> = _contactSearchQuery
    .combine(contactDao.getAllContacts()) { query, list ->
      if (query.isBlank()) list
      else list.filter {
        it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query)
      }
    }
    .distinctUntilChanged()
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val callHistory: StateFlow<List<CallRecord>> = callRecordDao.getAllCallRecords()
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val activeCall: StateFlow<ActiveCallSession?> = callingService.activeCall

  private val _backendNotice = MutableStateFlow<BackendNoticeState?>(null)
  val backendNotice: StateFlow<BackendNoticeState?> = _backendNotice.asStateFlow()

  val currentUser: StateFlow<UserAccount?> = userProfileService.currentUser

  private val _selectedContactDetail = MutableStateFlow<Contact?>(null)
  val selectedContactDetail: StateFlow<Contact?> = _selectedContactDetail.asStateFlow()

  private val _showAddContactScreen = MutableStateFlow(false)
  val showAddContactScreen: StateFlow<Boolean> = _showAddContactScreen.asStateFlow()

  private val _missedCallBannerDismissed = MutableStateFlow(false)
  val missedCallBannerDismissed: StateFlow<Boolean> = _missedCallBannerDismissed.asStateFlow()

  private val _recentsFilterType = MutableStateFlow<CallType?>(null)
  val recentsFilterType: StateFlow<CallType?> = _recentsFilterType.asStateFlow()

  private val _favoritesViewAsList = MutableStateFlow(false)
  val favoritesViewAsList: StateFlow<Boolean> = _favoritesViewAsList.asStateFlow()

  init {
    ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
    viewModelScope.launch {
      userProfileService.currentUser.collect { user ->
        if (user != null) syncFcmToken(user)
      }
    }
    viewModelScope.launch {
      callingService.callEvents.collect { event ->
        if (event is CallEvent.Error) {
          showNotice("Call error", event.message, _enteredNumber.value)
        }
      }
    }
    // Use the encrypted local session to choose the first screen immediately.
    // Network refresh/profile loading happens after the main UI is already visible.
    val hasSession = supabase.hasStoredSession()
    _currentScreen.value = if (hasSession) ScreenState.MAIN_APP else ScreenState.WELCOME

    if (hasSession) {
      viewModelScope.launch {
        var restoredUser: UserAccount? = null
        while (restoredUser == null && supabase.hasStoredSession()) {
          try {
            restoredUser = authService.restoreSession()
            if (restoredUser == null) {
              _currentScreen.value = ScreenState.WELCOME
              return@launch
            }
          } catch (error: Exception) {
            // A temporary network failure must not send a returning user to onboarding.
            Log.w("LineCallAuth", "Session restoration failed temporarily; retrying in background.", error)
            delay(3_000L)
          }
        }
        if (restoredUser != null) {
          userProfileService.setUser(restoredUser)
          _emailInput.value = restoredUser.email
          _phoneInput.value = restoredUser.phoneNumber
          _displayNameInput.value = restoredUser.displayName
          startIncomingCallPollingIfForeground()
        } else {
          _currentScreen.value = ScreenState.WELCOME
        }
      }
    }

  }

  fun navigateTo(screen: ScreenState) {
    _currentScreen.value = screen
  }

  fun selectTab(tab: MainTab) {
    _selectedTab.value = tab
  }

  fun updateEmailInput(email: String) {
    _emailInput.value = email
    _authError.value = null
    _authNotice.value = null
  }

  fun checkEmailAndContinue() {
    if (_isAuthBusy.value) return
    val email = _emailInput.value.trim()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      _authError.value = "Enter a valid email address."
      return
    }
    viewModelScope.launch {
      _isAuthBusy.value = true
      _authError.value = null
      _authNotice.value = null
      supabase.checkEmailExists(email).onSuccess { account ->
        _passwordInput.value = ""
        _confirmPasswordInput.value = ""
        if (account.exists) {
          isCompletingRegistration = false
          _loginIdentifier.value = email
          _loginDisplayName.value = account.displayName
          _loginAvatarUrl.value = account.avatarUrl
          _authNotice.value = null
          _currentScreen.value = ScreenState.LOGIN
        } else {
          isCompletingRegistration = true
          _loginDisplayName.value = ""
          _loginAvatarUrl.value = null
          _currentScreen.value = ScreenState.PASSWORD_SETUP
        }
      }.onFailure {
        _authError.value = it.message ?: "We couldn't check this email. Please try again."
      }
      _isAuthBusy.value = false
    }
  }

  fun continueToPasswordSetup() {
    val email = _emailInput.value.trim()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      _authError.value = "Enter a valid email address."
      return
    }
    _authError.value = null
    _passwordInput.value = ""
    _confirmPasswordInput.value = ""
    _currentScreen.value = ScreenState.PASSWORD_SETUP
  }

  fun registerAccount() {
    val email = _emailInput.value.trim()
    val password = _passwordInput.value
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      _authError.value = "Enter a valid email address."
      return
    }
    if (password.length < 8) {
      _authError.value = "Use at least 8 characters for your password."
      return
    }
    if (password != _confirmPasswordInput.value) {
      _authError.value = "The passwords do not match."
      return
    }
    // Keep registration details locally. No Auth user exists until phone is supplied.
    isCompletingRegistration = true
    _phoneInput.value = ""
    _authError.value = null
    _currentScreen.value = ScreenState.PHONE_SETUP
  }

  fun updateLoginIdentifier(value: String) { _loginIdentifier.value = value; _authError.value = null; _authNotice.value = null }
  fun updatePasswordInput(value: String) { _passwordInput.value = value; _authError.value = null }
  fun updateConfirmPasswordInput(value: String) { _confirmPasswordInput.value = value; _authError.value = null }

  fun signIn() {
    val identifier = _loginIdentifier.value.trim()
    val password = _passwordInput.value
    if (identifier.isBlank() || password.isBlank()) {
      _authError.value = "Enter your email or phone number and password."
      return
    }
    viewModelScope.launch {
      _isAuthBusy.value = true
      val result = authService.signIn(identifier, password)
      result.onSuccess { user ->
        userProfileService.setUser(user)
        _emailInput.value = user.email
        _phoneInput.value = user.phoneNumber
        _displayNameInput.value = user.displayName
        _passwordInput.value = ""
        // Successful authentication must not send an existing account back
        // through registration screens, even if optional profile details are absent.
        _currentScreen.value = ScreenState.MAIN_APP
        startIncomingCallPollingIfForeground()
        _authError.value = null
        _authNotice.value = null
      }.onFailure { _authError.value = it.message ?: "Could not sign in. Check your details." }
      _isAuthBusy.value = false
    }
  }

  fun updateRecoveryEmail(value: String) { _recoveryEmail.value = value; _authError.value = null }
  fun updateRecoveryPhone(value: String) { _recoveryPhone.value = value; _authError.value = null }
  fun updateNewPassword(value: String) { _newPassword.value = value; _authError.value = null }
  fun updateConfirmNewPassword(value: String) { _confirmNewPassword.value = value; _authError.value = null }

  fun requestPasswordRecovery() {
    val email = _recoveryEmail.value.trim()
    val phone = _recoveryPhone.value.trim()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      _authError.value = "Enter the email address registered to your account."
      return
    }
    if (!phone.startsWith("+") || !phone.drop(1).matches(Regex("^[1-9][0-9]{6,14}$"))) {
      _authError.value = "Enter your registered phone number with its country code, for example +2348012345678."
      return
    }
    viewModelScope.launch {
      _isAuthBusy.value = true
      val result = supabase.requestPasswordRecovery(email, phone)
      result.onSuccess { token ->
        _recoveryTokenHash.value = token
        _newPassword.value = ""
        _confirmNewPassword.value = ""
        _authError.value = null
        _currentScreen.value = ScreenState.RESET_PASSWORD
      }.onFailure { _authError.value = it.message ?: "The email and phone number could not be matched." }
      _isAuthBusy.value = false
    }
  }

  fun completePasswordRecovery() {
    val password = _newPassword.value
    if (password.length < 8) {
      _authError.value = "Use at least 8 characters for your new password."
      return
    }
    if (password != _confirmNewPassword.value) {
      _authError.value = "The new passwords do not match."
      return
    }
    viewModelScope.launch {
      _isAuthBusy.value = true
      val result = supabase.completePasswordRecovery(_recoveryTokenHash.value, password)
      result.onSuccess {
        _recoveryTokenHash.value = ""
        _recoveryEmail.value = ""
        _recoveryPhone.value = ""
        _newPassword.value = ""
        _confirmNewPassword.value = ""
        _loginIdentifier.value = ""
        _passwordInput.value = ""
        _authError.value = null
        _currentScreen.value = ScreenState.LOGIN
      }.onFailure { _authError.value = it.message ?: "Could not reset your password. Please try again." }
      _isAuthBusy.value = false
    }
  }

  fun selectCountry(country: Country) {
    _selectedCountry.value = country
  }

  fun updatePhoneInput(phone: String) {
    _phoneInput.value = phone
    _authError.value = null
  }

  fun submitPhoneNumber() {
    val e164 = userProfileService.formatToE164(_selectedCountry.value.dialCode, _phoneInput.value)
    if (!userProfileService.validateE164(e164)) { _authError.value = "Enter a valid international phone number."; return }
    viewModelScope.launch {
      _isAuthBusy.value = true
      if (isCompletingRegistration) {
        val result = authService.register(_emailInput.value.trim(), _passwordInput.value, e164)
        result.onSuccess { user ->
          isCompletingRegistration = false
          userProfileService.setUser(user)
          _emailInput.value = user.email
          _phoneInput.value = user.phoneNumber
          _displayNameInput.value = user.displayName
          _passwordInput.value = ""
          _confirmPasswordInput.value = ""
          _authError.value = null
          _currentScreen.value = ScreenState.PROFILE_SETUP
        }.onFailure { _authError.value = it.message ?: "Could not create your account. No account was created." }
      } else {
        val result = userProfileService.registerPhoneNumber(e164)
        result.onSuccess { user -> userProfileService.setUser(user); _phoneInput.value = user.phoneNumber; _authError.value = null; _currentScreen.value = ScreenState.PROFILE_SETUP }
          .onFailure { _authError.value = it.message ?: "Could not save this phone number." }
      }
      _isAuthBusy.value = false
    }
  }

  fun goToLoginFromRegistration() {
    isCompletingRegistration = false
    _loginIdentifier.value = _emailInput.value.trim()
    _passwordInput.value = ""
    _authError.value = null
    _currentScreen.value = ScreenState.LOGIN
  }

  fun backFromPhoneSetup() {
    _currentScreen.value = if (isCompletingRegistration) ScreenState.PASSWORD_SETUP else ScreenState.WELCOME
    _authError.value = null
  }

  fun updateDisplayNameInput(name: String) {
    _displayNameInput.value = name
    _authError.value = null
  }

  fun saveDisplayName(name: String) {
    _displayNameInput.value = name
    viewModelScope.launch {
      val result = userProfileService.updateProfile(name)
      result.onFailure {
        _backendNotice.value = BackendNoticeState(
          title = "Profile not saved",
          message = it.message ?: "Your display name could not be updated.",
          targetNumber = "",
          targetName = null
        )
      }
    }
  }

  fun updateProfilePhoneNumber(phoneNumber: String) {
    viewModelScope.launch {
      val result = userProfileService.updatePhoneNumber(phoneNumber.trim())
      result.onSuccess { _phoneInput.value = it.phoneNumber }
      result.onFailure {
        _backendNotice.value = BackendNoticeState(
          title = "Phone number not saved",
          message = it.message ?: "Your phone number could not be updated.",
          targetNumber = "",
          targetName = null
        )
      }
    }
  }

  fun updateProfileEmail(email: String) {
    viewModelScope.launch {
      emailBeforeChange = userProfileService.currentUser.value?.email.orEmpty()
      val result = userProfileService.updateEmail(email)
      result.onSuccess {
        emailChangeAwaitingConfirmation = true
        _backendNotice.value = BackendNoticeState(
          title = "Confirm your new email",
          message = "A confirmation link has been sent to the new email address. Your Line Call email will change after you confirm it. Until then, keep signing in with your current email.",
          targetNumber = "",
          targetName = null
        )
      }.onFailure {
        _backendNotice.value = BackendNoticeState(
          title = "Email change not requested",
          message = it.message ?: "Your email address could not be changed.",
          targetNumber = "",
          targetName = null
        )
      }
    }
  }

  fun refreshProfileAfterAppResume() {
    if (!emailChangeAwaitingConfirmation) return
    viewModelScope.launch {
      userProfileService.refreshCurrentUser().onSuccess { refreshed ->
        if (refreshed.email.isNotBlank()) {
          _emailInput.value = refreshed.email
          _loginIdentifier.value = refreshed.email
        }
        if (refreshed.email.isNotBlank() && !refreshed.email.equals(emailBeforeChange, ignoreCase = true)) {
          emailChangeAwaitingConfirmation = false
        }
      }
    }
  }

  fun updateProfileAvatar(uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val resolver = getApplication<Application>().contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
          throw IllegalArgumentException("The selected image could not be opened.")
        }
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > 1024 || bounds.outHeight / sampleSize > 1024) {
          sampleSize *= 2
        }
        val decoded = resolver.openInputStream(uri)?.use {
          BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: throw IllegalArgumentException("The selected image could not be opened.")
        val side = minOf(decoded.width, decoded.height)
        val square = Bitmap.createBitmap(
          decoded,
          (decoded.width - side) / 2,
          (decoded.height - side) / 2,
          side,
          side
        )
        val avatar = Bitmap.createScaledBitmap(square, 512, 512, true)
        val bytes = ByteArrayOutputStream().use { output ->
          avatar.compress(Bitmap.CompressFormat.JPEG, 86, output)
          output.toByteArray()
        }
        if (avatar !== square) avatar.recycle()
        if (square !== decoded) square.recycle()
        decoded.recycle()
        val result = userProfileService.updateAvatar(bytes)
        result.onFailure {
          _backendNotice.value = BackendNoticeState(
            title = "Photo not saved",
            message = it.message ?: "Your profile photo could not be uploaded.",
            targetNumber = "",
            targetName = null
          )
        }
      } catch (error: Exception) {
        _backendNotice.value = BackendNoticeState(
          title = "Photo not saved",
          message = error.message ?: "The selected photo could not be processed.",
          targetNumber = "",
          targetName = null
        )
      }
    }
  }

  fun completeProfileSetup() {
    viewModelScope.launch {
      if (_displayNameInput.value.isNotBlank()) {
        val result = userProfileService.updateProfile(_displayNameInput.value)
        if (result.isFailure) {
          _authError.value = result.exceptionOrNull()?.message ?: "Could not save your profile."
          return@launch
        }
      }
      enterMainApp()
    }
  }

  fun skipProfileSetup() {
    enterMainApp()
  }

  private fun startIncomingCallPollingIfForeground() {
    if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
      callingService.startIncomingCallPolling()
    }
  }

  private fun enterMainApp() {
    _currentScreen.value = ScreenState.MAIN_APP
    startIncomingCallPollingIfForeground()
  }

  fun appendKeypadChar(char: Char) {
    if (_enteredNumber.value.length < 25) _enteredNumber.value += char
  }

  fun backspaceKeypad() {
    if (_enteredNumber.value.isNotEmpty()) _enteredNumber.value = _enteredNumber.value.dropLast(1)
  }

  fun clearKeypad() {
    _enteredNumber.value = ""
  }

  fun setEnteredNumber(number: String) {
    _enteredNumber.value = number
  }

  fun onCallButtonPressed() {
    val rawNumber = _enteredNumber.value.trim()
    if (rawNumber.isBlank()) {
      showNotice("Enter a phone number", "Type the registered Line Call number you want to reach.", rawNumber)
      return
    }
    val e164 = if (rawNumber.startsWith("+")) {
      rawNumber.replace(Regex("[^0-9+]"), "")
    } else {
      userProfileService.formatToE164(_selectedCountry.value.dialCode, rawNumber)
    }

    viewModelScope.launch {
      when (val result = callingService.initiateCall(e164, null)) {
        is CallInitiationResult.Started -> Unit
        is CallInitiationResult.BackendNotConnected ->
          showNotice("Call could not start", result.message, e164)
        is CallInitiationResult.InvalidNumber ->
          showNotice("Invalid phone number", result.reason, rawNumber)
        is CallInitiationResult.UserNotRegistered ->
          showNotice("Not on Line Call", "This number is not registered on Line Call yet.", e164)
      }
    }
  }

  private fun syncFcmToken(user: UserAccount) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
      if (!task.isSuccessful) {
        Log.w("LineCallFCM", "Could not obtain this device's FCM token.", task.exception)
        return@addOnCompleteListener
      }
      val token = task.result?.takeIf { it.isNotBlank() } ?: return@addOnCompleteListener
      viewModelScope.launch(Dispatchers.IO) {
        runCatching { supabase.saveFcmToken(user.id, token) }
          .onFailure { Log.w("LineCallFCM", "Could not save this device's FCM token.", it) }
      }
    }
  }

  fun handleIncomingCallAction(action: String, callId: String) {
    if (action != "answer" && action != "decline") return
    viewModelScope.launch {
      repeat(40) {
        val session = callingService.activeCall.value
        if (session?.callId == callId && session.isIncoming) {
          if (action == "answer") callingService.acceptIncomingCall()
          else callingService.declineIncomingCall()
          return@launch
        }
        delay(500)
      }
    }
  }

  fun acceptIncomingCall() = callingService.acceptIncomingCall()
  fun declineIncomingCall() = callingService.declineIncomingCall()
  fun endCall() = callingService.endCall()
  fun toggleMute() = callingService.toggleMute()
  fun toggleSpeaker() = callingService.toggleSpeaker()

  fun dismissBackendNotice() {
    _backendNotice.value = null
  }

  private fun showNotice(title: String, message: String, number: String, name: String? = null) {
    _backendNotice.value = BackendNoticeState(title, message, number, name)
  }

  fun selectContactDetail(contact: Contact?) {
    _selectedContactDetail.value = contact
  }

  fun setShowAddContactScreen(show: Boolean) {
    _showAddContactScreen.value = show
  }

  fun dismissMissedCallBanner() {
    _missedCallBannerDismissed.value = true
  }

  fun setRecentsFilterType(type: CallType?) {
    _recentsFilterType.value = type
  }

  fun toggleFavoritesViewAsList() {
    _favoritesViewAsList.value = !_favoritesViewAsList.value
  }

  fun toggleFavorite(contact: Contact) {
    viewModelScope.launch {
      val updated = contact.copy(isFavorite = !contact.isFavorite)
      contactDao.updateContact(updated)
      if (_selectedContactDetail.value?.id == contact.id) {
        _selectedContactDetail.value = updated
      }
    }
  }

  fun updateContactSearch(query: String) {
    _contactSearchQuery.value = query
  }

  fun addContact(name: String, phoneNumber: String) {
    val cleanPhone = if (phoneNumber.startsWith("+")) phoneNumber
    else userProfileService.formatToE164(_selectedCountry.value.dialCode, phoneNumber)
    viewModelScope.launch {
      contactDao.insertContact(
        Contact(
          name = name.trim(),
          phoneNumber = cleanPhone,
          avatarInitials = name.trim().take(2).uppercase()
        )
      )
    }
  }

  fun deleteContact(contact: Contact) {
    viewModelScope.launch { contactDao.deleteContact(contact) }
  }

  fun callContact(contact: Contact) {
    _enteredNumber.value = contact.phoneNumber
    viewModelScope.launch {
      handleCallResult(callingService.initiateCall(contact.phoneNumber, contact.name), contact.phoneNumber, contact.name)
    }
  }

  fun callRecent(record: CallRecord) {
    _enteredNumber.value = record.phoneNumber
    viewModelScope.launch {
      handleCallResult(callingService.initiateCall(record.phoneNumber, record.contactName), record.phoneNumber, record.contactName)
    }
  }

  fun recallCall(session: ActiveCallSession) {
    callingService.dismissEndedCall()
    _enteredNumber.value = session.phoneNumber
    viewModelScope.launch {
      handleCallResult(
        callingService.initiateCall(session.phoneNumber, session.contactName),
        session.phoneNumber,
        session.contactName
      )
    }
  }

  private fun handleCallResult(result: CallInitiationResult, number: String, name: String?) {
    when (result) {
      is CallInitiationResult.Started -> Unit
      is CallInitiationResult.BackendNotConnected -> showNotice("Call unavailable", "We couldn’t place this call. Check your internet connection and try again.", number, name)
      is CallInitiationResult.InvalidNumber -> showNotice("Invalid number", "Enter a valid phone number and try again.", number, name)
      is CallInitiationResult.UserNotRegistered -> showNotice("Number not found", "The number you dialed isn’t registered on Line Call.", number, name)
    }
  }

  fun clearCallHistory() {
    viewModelScope.launch { callRecordDao.clearAllCallRecords() }
  }

  fun signOut() {
    viewModelScope.launch {
      callingService.stopIncomingCallPolling()
      userProfileService.currentUser.value?.let { user ->
        runCatching { supabase.clearFcmToken(user.id) }
          .onFailure { Log.w("LineCallFCM", "Could not clear device token during sign-out.", it) }
      }
      authService.signOut()
      userProfileService.clearUser()
      _currentScreen.value = ScreenState.WELCOME
      _emailInput.value = ""
      _phoneInput.value = ""
      _displayNameInput.value = ""
      _authError.value = null
      _authNotice.value = null
      _enteredNumber.value = ""
    }
  }

  override fun onCleared() {
    ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
    callingService.stopIncomingCallPolling()
    super.onCleared()
  }

}
