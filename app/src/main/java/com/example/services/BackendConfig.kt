package com.example.services

/** Public client configuration for the Line Call Supabase project. */
object BackendConfig {
  const val SUPABASE_URL = "https://ohytvswlxyttldmocjch.supabase.co"
  const val SUPABASE_ANON_KEY = "sb_publishable_qvp4AJPUqV3c8foFMxAPGw_YRWCVmW7"
  const val AUTH_REDIRECT_URL = "linecall://auth/callback"

  val isSupabaseConfigured: Boolean
    get() = SUPABASE_URL.isNotBlank() && SUPABASE_ANON_KEY.isNotBlank()

  // LiveKit credentials must remain server-side in Supabase Edge Function secrets.
  const val LIVEKIT_SERVER_URL = ""
  val isLiveKitConfigured: Boolean
    get() = LIVEKIT_SERVER_URL.isNotBlank()

  const val FCM_STATUS = "Firebase Cloud Messaging setup is pending"
}
