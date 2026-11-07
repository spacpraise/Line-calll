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
import com.example.services.AuthState
import com.example.services.CallInitiationResult
import com.example.services.CallingService
import com.example.services.LineCallAuthService
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

  val authService = LineCallAuthService()
  val userProfileService = UserProfileService(authService)
  val callingService = CallingService(callRecordDao, userProfileService, viewModelScope)

  // Current navigation screen
  private val _currentScreen = MutableStateFlow(ScreenState.WELCOME)
  val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

  // Main tab selection
  private val _selectedTab = MutableStateFlow(MainTab.KEYPAD)
  val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

  // Keypad entered number
  private val _enteredNumber = MutableStateFlow("")
  val enteredNumber: StateFlow<String> = _enteredNumber.asStateFlow()

  // Selected country in phone setup or dialer
  private val _selectedCountry = MutableStateFlow(CountryList.defaultCountry)
  val selectedCountry: StateFlow<Country> = _selectedCountry.asStateFlow()

  // Registration & Onboarding inputs
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

  // Contacts search and list
  private val _contactSearchQuery = MutableStateFlow("")
  val contactSearchQuery: StateFlow<String> = _contactSearchQuery.asStateFlow()

  val contacts: StateFlow<List<Contact>> = _contactSearchQuery
    .combine(contactDao.getAllContacts()) { query, list ->
      if (query.isBlank()) list
      else list.filter {
        it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query)
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Call history records
  val callHistory: StateFlow<List<CallRecord>> = callRecordDao.getAllCallRecords()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Calling state
  val activeCall: StateFlow<ActiveCallSession?> = callingService.activeCall

  // Honest backend notice dialog
  private val _backendNotice = MutableStateFlow<BackendNoticeState?>(null)
  val backendNotice: StateFlow<BackendNoticeState?> = _backendNotice.asStateFlow()

  val currentUser: StateFlow<UserAccount?> = userProfileService.currentUser

  init {
    // Seed initial demo contact if empty to make contacts list discoverable
    viewModelScope.launch {
      // Keep initial local contacts clean and practical
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
        _authError.value = result.exceptionOrNull()?.message ?: "Failed to send magic link"
      }
    }
  }

  fun verifyMagicLinkCallback() {
    viewModelScope.launch {
      val result = authService.handleAuthCallback(_emailInput.value)
      if (result.isSuccess) {
        _magicLinkSent.value = false
        _currentScreen.value = ScreenState.PHONE_SETUP
      } else {
        _authError.value = "Failed to verify magic link"
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
    val result = userProfileService.registerPhoneNumber(e164)
    if (result.isSuccess) {
      _authError.value = null
      _currentScreen.value = ScreenState.PROFILE_SETUP
    } else {
      _authError.value = result.exceptionOrNull()?.message ?: "Invalid phone number"
    }
  }

  fun updateDisplayNameInput(name: String) {
    _displayNameInput.value = name
  }

  fun completeProfileSetup() {
    if (_displayNameInput.value.isNotBlank()) {
      userProfileService.updateProfile(_displayNameInput.value, null)
    }
    _currentScreen.value = ScreenState.MAIN_APP
  }

  fun skipProfileSetup() {
    _currentScreen.value = ScreenState.MAIN_APP
  }

  // Keypad Actions
  fun appendKeypadChar(char: Char) {
    if (_enteredNumber.value.length < 25) {
      _enteredNumber.value += char
    }
  }

  fun backspaceKeypad() {
    if (_enteredNumber.value.isNotEmpty()) {
      _enteredNumber.value = _enteredNumber.value.dropLast(1)
    }
  }

  fun clearKeypad() {
    _enteredNumber.value = ""
  }

  fun setEnteredNumber(number: String) {
    _enteredNumber.value = number
  }

  // Calling Actions
  fun onCallButtonPressed() {
    val rawNumber = _enteredNumber.value.trim()
    if (rawNumber.isBlank()) return

    // Ensure E.164 formatting
    val e164 = if (rawNumber.startsWith("+")) {
      rawNumber.replace(Regex("[^0-9+]"), "")
    } else {
      userProfileService.formatToE164(_selectedCountry.value.dialCode, rawNumber)
    }

    viewModelScope.launch {
      val result = callingService.initiateCall(e164, null, forceDevelopmentTestMode = false)
      when (result) {
        is CallInitiationResult.Started -> {
          // Call initiated
        }
        is CallInitiationResult.BackendNotConnected -> {
          _backendNotice.value = BackendNoticeState(
            title = "Backend Service Required",
            message = result.message + "\n\nLine Call is designed to connect to Supabase Edge Functions for token authorization and LiveKit Cloud for real-time WebRTC audio.",
            targetNumber = e164,
            targetName = null
          )
        }
        is CallInitiationResult.InvalidNumber -> {
          _backendNotice.value = BackendNoticeState(
            title = "Invalid Number Format",
            message = result.reason,
            targetNumber = rawNumber,
            targetName = null
          )
        }
        is CallInitiationResult.UserNotRegistered -> {
          _backendNotice.value = BackendNoticeState(
            title = "User Not Registered",
            message = "This phone number ($e164) is not registered on Line Call.",
            targetNumber = e164,
            targetName = null
          )
        }
      }
    }
  }

  fun startTestCallPreview(e164: String, name: String?) {
    _backendNotice.value = null
    viewModelScope.launch {
      callingService.initiateCall(e164, name, forceDevelopmentTestMode = true)
    }
  }

  fun triggerIncomingCallSimulation() {
    callingService.triggerIncomingCallPreview(
      callerName = "Sarah Jenkins",
      callerPhone = "+1 (415) 555-0198"
    )
  }

  fun acceptIncomingCall() {
    callingService.acceptIncomingCall()
  }

  fun declineIncomingCall() {
    callingService.declineIncomingCall()
  }

  fun endCall() {
    callingService.endCall()
  }

  fun toggleMute() {
    callingService.toggleMute()
  }

  fun toggleSpeaker() {
    callingService.toggleSpeaker()
  }

  fun dismissBackendNotice() {
    _backendNotice.value = null
  }

  // Contacts Actions
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
    viewModelScope.launch {
      contactDao.deleteContact(contact)
    }
  }

  fun callContact(contact: Contact) {
    _enteredNumber.value = contact.phoneNumber
    viewModelScope.launch {
      val result = callingService.initiateCall(contact.phoneNumber, contact.name, forceDevelopmentTestMode = false)
      if (result is CallInitiationResult.BackendNotConnected) {
        _backendNotice.value = BackendNoticeState(
          title = "Backend Service Required",
          message = result.message,
          targetNumber = contact.phoneNumber,
          targetName = contact.name
        )
      }
    }
  }

  fun callRecent(record: CallRecord) {
    _enteredNumber.value = record.phoneNumber
    viewModelScope.launch {
      val result = callingService.initiateCall(record.phoneNumber, record.contactName, forceDevelopmentTestMode = false)
      if (result is CallInitiationResult.BackendNotConnected) {
        _backendNotice.value = BackendNoticeState(
          title = "Backend Service Required",
          message = result.message,
          targetNumber = record.phoneNumber,
          targetName = record.contactName
        )
      }
    }
  }

  fun clearCallHistory() {
    viewModelScope.launch {
      callRecordDao.clearAllCallRecords()
    }
  }

  fun signOut() {
    viewModelScope.launch {
      authService.signOut()
      _currentScreen.value = ScreenState.WELCOME
      _emailInput.value = ""
      _phoneInput.value = ""
      _displayNameInput.value = ""
      _magicLinkSent.value = false
    }
  }
}
