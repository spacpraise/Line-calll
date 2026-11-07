package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LineCallDatabase
import com.example.data.model.ActiveCallSession
import com.example.data.model.CallRecord
import com.example.data.model.Contact
import com.example.data.model.Country
import com.example.data.model.CountryList
import com.example.data.model.UserAccount
import com.example.services.CallInitiationResult
import com.example.services.CallingService
import com.example.services.LineCallAuthService
import com.example.services.LineCallSupabaseClient
import com.example.services.UserProfileService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenState {
  WELCOME,
  EMAIL_REGISTRATION,
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

  private val _currentScreen = MutableStateFlow(ScreenState.WELCOME)
  val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

  private val _selectedTab = MutableStateFlow(MainTab.KEYPAD)
  val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

  private val _enteredNumber = MutableStateFlow("")
  val enteredNumber: StateFlow<String> = _enteredNumber.asStateFlow()

  private val _selectedCountry = MutableStateFlow(CountryList.defaultCountry)
  val selectedCountry: StateFlow<Country> = _selectedCountry.asStateFlow()

  private val _emailInput = MutableStateFlow("")
  val emailInput: StateFlow<String> = _emailInput.asStateFlow()

  private val _phoneInput = MutableStateFlow("")
  val phoneInput: StateFlow<String> = _phoneInput.asStateFlow()

  private val _displayNameInput = MutableStateFlow("")
  val displayNameInput: StateFlow<String> = _displayNameInput.asStateFlow()

  private val _magicLinkSent = MutableStateFlow(false)
  val magicLinkSent: StateFlow<Boolean> = _magicLinkSent.asStateFlow()

  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  private val _contactSearchQuery = MutableStateFlow("")
  val contactSearchQuery: StateFlow<String> = _contactSearchQuery.asStateFlow()

  val contacts: StateFlow<List<Contact>> = _contactSearchQuery
    .combine(contactDao.getAllContacts()) { query, list ->
      if (query.isBlank()) list
      else list.filter {
        it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query)
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val callHistory: StateFlow<List<CallRecord>> = callRecordDao.getAllCallRecords()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val activeCall: StateFlow<ActiveCallSession?> = callingService.activeCall

  private val _backendNotice = MutableStateFlow<BackendNoticeState?>(null)
  val backendNotice: StateFlow<BackendNoticeState?> = _backendNotice.asStateFlow()

  val currentUser: StateFlow<UserAccount?> = userProfileService.currentUser

  init {
    viewModelScope.launch {
      val restoredUser = authService.restoreSession()
      if (restoredUser != null) {
        userProfileService.setUser(restoredUser)
        _emailInput.value = restoredUser.email
        _phoneInput.value = restoredUser.phoneNumber
        _displayNameInput.value = restoredUser.displayName
        if (restoredUser.phoneNumber.isBlank()) {
          _currentScreen.value = ScreenState.PHONE_SETUP
        } else {
          _currentScreen.value = ScreenState.MAIN_APP
          callingService.startIncomingCallPolling()
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
  }

  fun submitEmail() {
    viewModelScope.launch {
      val result = authService.sendMagicLink(_emailInput.value)
      if (result.isSuccess) {
        _magicLinkSent.value = true
        _authError.value = null
      } else {
        _authError.value = result.exceptionOrNull()?.message ?: "Could not send the sign-in link."
      }
    }
  }

  /** Called automatically when the Supabase email link opens linecall://auth/callback. */
  fun verifyMagicLinkCallback(callbackUrl: String) {
    viewModelScope.launch {
      val result = authService.handleAuthCallback(callbackUrl)
      result.onSuccess { user ->
        userProfileService.setUser(user)
        _magicLinkSent.value = false
        _authError.value = null
        _phoneInput.value = user.phoneNumber
        _displayNameInput.value = user.displayName
        if (user.phoneNumber.isBlank()) {
          _currentScreen.value = ScreenState.PHONE_SETUP
        } else {
          _currentScreen.value = ScreenState.MAIN_APP
          callingService.startIncomingCallPolling()
        }
      }.onFailure {
        _authError.value = it.message ?: "The sign-in link could not be verified. Request a new link."
        _currentScreen.value = ScreenState.EMAIL_REGISTRATION
      }
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
    viewModelScope.launch {
      val result = userProfileService.registerPhoneNumber(e164)
      result.onSuccess { user ->
        userProfileService.setUser(user)
        _phoneInput.value = user.phoneNumber
        _authError.value = null
        _currentScreen.value = ScreenState.PROFILE_SETUP
      }.onFailure {
        _authError.value = it.message ?: "Could not save this phone number."
      }
    }
  }

  fun updateDisplayNameInput(name: String) {
    _displayNameInput.value = name
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

  private fun enterMainApp() {
    _currentScreen.value = ScreenState.MAIN_APP
    callingService.startIncomingCallPolling()
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

  private fun handleCallResult(result: CallInitiationResult, number: String, name: String?) {
    when (result) {
      is CallInitiationResult.Started -> Unit
      is CallInitiationResult.BackendNotConnected -> showNotice("Call could not start", result.message, number, name)
      is CallInitiationResult.InvalidNumber -> showNotice("Invalid phone number", result.reason, number, name)
      is CallInitiationResult.UserNotRegistered -> showNotice("Not on Line Call", "This number is not registered on Line Call yet.", number, name)
    }
  }

  fun clearCallHistory() {
    viewModelScope.launch { callRecordDao.clearAllCallRecords() }
  }

  fun signOut() {
    viewModelScope.launch {
      callingService.stopIncomingCallPolling()
      authService.signOut()
      userProfileService.setUser(nullUserPlaceholder())
      _currentScreen.value = ScreenState.WELCOME
      _emailInput.value = ""
      _phoneInput.value = ""
      _displayNameInput.value = ""
      _magicLinkSent.value = false
      _authError.value = null
      _enteredNumber.value = ""
    }
  }

  private fun nullUserPlaceholder(): UserAccount {
    // UserProfileService has no separate clear method yet; the auth state is cleared on sign out.
    return UserAccount("", "", "", false, "", null, false)
  }
}
