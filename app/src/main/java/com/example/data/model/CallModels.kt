package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CallType {
  INCOMING,
  OUTGOING,
  MISSED
}

enum class CallSessionState {
  IDLE,
  DIALING,
  RINGING,
  CONNECTED,
  ENDED
}

@Entity(tableName = "call_records")
data class CallRecord(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val phoneNumber: String,
  val contactName: String?,
  val type: CallType,
  val timestamp: Long = System.currentTimeMillis(),
  val durationSeconds: Int = 0
) {
  val formattedDuration: String
    get() {
      if (durationSeconds <= 0) return "0s"
      val minutes = durationSeconds / 60
      val seconds = durationSeconds % 60
      return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

  val formattedTime: String
    get() {
      val now = System.currentTimeMillis()
      val diffDays = (now - timestamp) / (1000 * 60 * 60 * 24)
      val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
      val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
      return when {
        diffDays == 0L -> "Today, ${timeFormat.format(Date(timestamp))}"
        diffDays == 1L -> "Yesterday, ${timeFormat.format(Date(timestamp))}"
        else -> "${dateFormat.format(Date(timestamp))}, ${timeFormat.format(Date(timestamp))}"
      }
    }
}

@Entity(tableName = "contacts")
data class Contact(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val phoneNumber: String, // E.164 format
  val avatarInitials: String = name.take(2).uppercase(),
  val createdAt: Long = System.currentTimeMillis()
)

data class UserAccount(
  val id: String,
  val email: String,
  val phoneNumber: String, // E.164
  val isPhoneVerified: Boolean = false, // Phone ownership verification is a separate future capability
  val displayName: String = "",
  val avatarUrl: String? = null,
  val isRegisteredOnBackend: Boolean = false
)

data class ActiveCallSession(
  val callId: String,
  val contactName: String?,
  val phoneNumber: String,
  val state: CallSessionState,
  val isMuted: Boolean = false,
  val isSpeakerOn: Boolean = false,
  val durationSeconds: Int = 0,
  val isIncoming: Boolean = false
)
