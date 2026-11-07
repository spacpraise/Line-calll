# Line Call

Line Call is a native Android internet voice-calling app built with Kotlin and Jetpack Compose.

## Current architecture

- Android UI: Kotlin, Jetpack Compose, Room
- Authentication and user profiles: Supabase Auth + PostgREST
- Email sign-in: Supabase Magic Link using PKCE and Android deep links
- Call invitations and call state: Supabase Edge Function `call-control`
- Voice media: LiveKit Android SDK (WebRTC)
- Incoming-call push alerts: Firebase Cloud Messaging
- Phone numbers: E.164 format; phone ownership is explicitly unverified in this release

## Supabase setup

The app is configured for the existing Supabase project. In **Authentication → URL Configuration → Redirect URLs**, add:

```text
linecall://auth/callback
```

The deployed `call-control` Edge Function uses the Supabase service-role environment variable server-side. Never put a service-role key in the Android app.

## LiveKit setup

Create a LiveKit Cloud project and add these Edge Function secrets in Supabase:

- `LIVEKIT_URL` — the project's secure WebSocket URL, such as `wss://your-project.livekit.cloud`
- `LIVEKIT_API_KEY`
- `LIVEKIT_API_SECRET`

These credentials must remain server-side. The Edge Function issues short-lived, participant-specific room tokens.

## Firebase push setup

Create a Firebase project and register an Android app with this exact package name:

```text
com.aistudio.linecall.vczr
```

Download `google-services.json` and place it at:

```text
app/google-services.json
```

Enable Firebase Cloud Messaging. Create a service account with permission to send FCM messages, then add the complete service-account JSON as the Supabase Edge Function secret:

- `FIREBASE_SERVICE_ACCOUNT_JSON`

The app registers its FCM token against the signed-in user's profile. The Edge Function sends high-priority incoming-call data messages to that token. The app also polls for incoming calls while it is open, so foreground calling can be tested before push configuration is finished.

## User flow

1. User enters an email address.
2. Supabase sends a magic link.
3. The link opens `linecall://auth/callback` and completes PKCE sign-in in the app.
4. User adds an international phone number. No SMS verification is performed; the profile is marked unverified.
5. Users call other registered Line Call numbers.
6. The recipient accepts or declines; accepted calls join a private LiveKit room.

## Build

Open the project in Android Studio or Google AI Studio. The GitHub Actions workflow also runs:

```bash
gradle :app:assembleDebug
```

## Important limitations

- A recipient must have a Line Call account and internet access.
- Without Firebase configuration and the `FIREBASE_SERVICE_ACCOUNT_JSON` secret, incoming calls are only discovered while the recipient app is open.
- Without the three LiveKit secrets, call invitations work but audio rooms cannot connect.
- Phone-number ownership is not verified. Users should only register numbers they control.
- This release is voice-only; it does not connect to the public telephone network or call ordinary GSM numbers.
