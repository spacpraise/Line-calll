package com.example.services

/**
 * Backend integration configuration and service readiness indicators for Line Call.
 *
 * Line Call uses a modular backend architecture:
 * 1. Supabase Auth: Passwordless Email Magic Link authentication.
 * 2. Supabase PostgreSQL: User registry, phone number bindings, and contacts sync.
 * 3. Supabase Edge Functions: Secure call signaling, token issuance, and call invitations.
 * 4. LiveKit Cloud: Low-latency WebRTC internet audio communication.
 * 5. Firebase Cloud Messaging: Device wake-up for incoming call rings.
 *
 * To connect production services:
 * - Configure SUPABASE_URL and SUPABASE_ANON_KEY in your environment/secrets.
 * - Configure LIVEKIT_URL and LIVEKIT_API_KEY in Supabase Edge Functions.
 */
object BackendConfig {
  // Production URL and Key placeholders (to be set via BuildConfig / Secrets in production)
  const val SUPABASE_URL: String = ""
  const val SUPABASE_ANON_KEY: String = ""
  const val LIVEKIT_SERVER_URL: String = ""

  val isSupabaseConfigured: Boolean
    get() = SUPABASE_URL.isNotBlank() && SUPABASE_ANON_KEY.isNotBlank()

  val isLiveKitConfigured: Boolean
    get() = LIVEKIT_SERVER_URL.isNotBlank()

  const val FCM_STATUS: String = "Ready for google-services.json integration"

  fun getBackendStatusSummary(): List<ServiceStatusItem> {
    return listOf(
      ServiceStatusItem(
        serviceName = "Supabase Auth",
        description = "Passwordless Magic Link Email Sign-In",
        isReady = isSupabaseConfigured,
        setupInstruction = "Provide SUPABASE_URL and SUPABASE_ANON_KEY in build configuration"
      ),
      ServiceStatusItem(
        serviceName = "Supabase PostgreSQL",
        description = "User accounts and E.164 phone directory",
        isReady = isSupabaseConfigured,
        setupInstruction = "Deploy users and phone_numbers tables to Supabase"
      ),
      ServiceStatusItem(
        serviceName = "LiveKit Cloud WebRTC",
        description = "Real-time internet voice calling room server",
        isReady = isLiveKitConfigured,
        setupInstruction = "Configure LiveKit room token generator in Edge Functions"
      ),
      ServiceStatusItem(
        serviceName = "Firebase Cloud Messaging",
        description = "Background incoming call wake-up and ring notifications",
        isReady = false,
        setupInstruction = "Add google-services.json and FCM token handler"
      )
    )
  }
}

data class ServiceStatusItem(
  val serviceName: String,
  val description: String,
  val isReady: Boolean,
  val setupInstruction: String
)
