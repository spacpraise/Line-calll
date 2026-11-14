# Line Call

Line Call is a native Android internet voice-calling application built with Kotlin and Jetpack Compose.

## Current app capabilities

- Email/password registration and login, plus phone-number login through server-side Supabase functions.
- User profiles, local contacts, favorites, and call history.
- LiveKit/WebRTC voice calls with default Opus negotiation and WebRTC congestion control.
- Android Core-Telecom integration for system call surfaces, audio focus, call lifecycle, and supported audio endpoints.
- Incoming-call CallStyle notifications with FCM delivery, a bounded ringing service, and server-state validation.
- Ongoing-call foreground service for active calls.
- Audio route selection for earpiece, speaker, wired/USB audio, and supported Bluetooth endpoints.
- Native WebRTC audio processing with platform echo cancellation/noise suppression when available and WebRTC fallback processing.
- Network-aware diagnostics: network transport, validated connectivity, link bandwidth estimates, round-trip latency, audio jitter, packet loss, and measured audio throughput.
- Automatic LiveKit ICE recovery and reconnection handling; the UI exposes reconnecting and connection-quality states.
- Light, dark, and system appearance modes.
- Background release checks and in-app APK update installation after Android grants the required install permission.

## Android call integration

The app registers calls with AndroidX Core-Telecom. Android remains the authority for system call UI, call endpoints, mute/hold state, and audio routing while a Telecom call is active. The app does not fight Telecom by changing AudioManager routes during a managed call.

Some remote controls, Bluetooth endpoint names, and lock-screen surfaces depend on Android version, OEM behavior, device support, and user permissions. Full-screen incoming-call UI is shown only when Android permits it; users can manage that permission in Android settings.

## Voice and network quality

LiveKit Android SDK 2.27.0 supplies the WebRTC voice engine. The app uses the native voice-communication audio path and its platform/WebRTC processing rather than layering a second custom DSP engine over it. Available platform effects and behavior vary by Android version and device manufacturer.

The app monitors network conditions for visibility and recovery. LiveKit/WebRTC retain control of ICE candidate selection, reconnection, congestion control, and adaptive media behavior. The app does not hard-code a codec or fixed bitrate. Network metrics are diagnostic estimates, not guarantees of call quality.

## Backend configuration

The Android app uses the existing Supabase project. Keep service-role credentials and LiveKit API secrets on the server; never embed them in the Android APK.

Required server-side configuration includes the deployed Supabase Edge Functions and their secrets:

- `call-control` — call initiation, status, answer/decline/end, and call-session operations.
- `phone-login` — private phone-number lookup and password sign-in.
- `register-account` — account and profile creation after registration is complete.
- `password-recovery` — validates the registered email and phone before issuing a recovery token.
- LiveKit URL, API key, and API secret for server-issued room tokens.
- Firebase Cloud Messaging credentials used by the server to send incoming-call data messages.

The Android project includes `app/google-services.json`; use the matching Firebase project when building and deploying. FCM delivery and full-screen call presentation remain subject to Android background execution, notification, and device-vendor policies.

## Permissions

- Microphone: required to transmit voice during calls; requested when the user starts or answers a call.
- Notifications: needed for visible incoming-call and update alerts on Android 13+.
- Full-screen intent: Android may require the user to enable this for lock-screen incoming-call presentation.
- Contacts: optional; used to import names and numbers from the address book.
- Bluetooth connect: optional on Android 12+; improves identification of multiple paired call-audio endpoints. Basic call audio remains available without it.
- Internet and network state: required for account, signalling, media, and network monitoring.
- Foreground service and wake lock permissions: used for bounded incoming-call handling and active calls.

The app includes a Settings shortcut to Android's app-permission screen. Users can revoke optional permissions at any time.

## Build and test

Use Android Studio or the repository's GitHub Actions workflows.

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
gradle :app:assembleDebugAndroidTest
gradle :app:connectedDebugAndroidTest
```

Pushes to `main` and pull requests run the debug APK build, JVM unit tests, instrumentation APK build, and Android emulator UI regression tests.

## Release

The current release candidate is **1.02** (versionCode **10200**). See [RELEASING.md](RELEASING.md) for signing and publication steps.

## Important limitations

- Line Call is an internet VoIP app. It does not place ordinary cellular/PSTN calls.
- Both participants need Line Call accounts, internet access, and a supported device/app configuration.
- Reliable incoming-call presentation depends on FCM delivery, Android notification settings, battery/background policies, and OEM restrictions. Android can suppress full-screen presentation.
- Bluetooth headset controls, wearable controls, and Android Auto integration vary by Android release and connected device; only controls exposed through Android Telecom are available.
- Network metrics can be unavailable briefly during call setup, reconnection, or when the peer connection has not produced enough RTP statistics.
- Real-world audio quality, Bluetooth routing, background delivery, and network handover still require physical-device testing across Android versions and manufacturers.