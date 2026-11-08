# Line Call

Line Call is a native Android internet voice-calling app built with Kotlin and Jetpack Compose.

## Current architecture

- Android UI: Kotlin, Jetpack Compose, Room
- Authentication and user profiles: Supabase Auth + PostgREST
- Authentication: Supabase email-and-password sign-up and sign-in, plus server-mediated phone-number-and-password sign-in
- Call invitations and call state: Supabase Edge Function `call-control`
- Voice media: LiveKit Android SDK (WebRTC)
- Incoming-call detection: Supabase polling while the app is open (no push notifications yet)
- Phone numbers: E.164 format; phone ownership is explicitly unverified in this release

## Supabase setup

Use the existing Supabase project configured in the Android app.

For the current registration flow, turn **Confirm email OFF** in Supabase Dashboard → Authentication → Providers → Email. The app expects password sign-up to return an authenticated session immediately. If Supabase returns no session, the app will show a configuration error rather than pretending registration succeeded.

The app does not use magic links, PKCE email callbacks, or an email-verification screen. No redirect URL is required for email authentication. The login identifier accepts either the registered email address or the registered phone number in E.164 format (for example, `+2348012345678`). Phone login is handled by the `phone-login` Edge Function: it resolves the phone number privately on the server, then delegates password verification and session issuance to Supabase Auth. The function never returns the account email to the device.

The deployed `call-control`, `phone-login`, and `password-recovery` Edge Functions use the Supabase service-role environment variable server-side. Never put a service-role key in the Android app. Password recovery requires an exact match of the registered email and E.164 phone number in `public.profiles`; only then does the function issue a short-lived Supabase recovery token. The app verifies that token and lets the user set a new password without sending an email or SMS. Attempts are rate-limited using hashed email/phone and IP identifiers in the RLS-protected `password_reset_attempts` table.

## LiveKit setup

Create a LiveKit Cloud project and add these Edge Function secrets in Supabase:

- `LIVEKIT_URL` — the project's secure WebSocket URL, such as `wss://your-project.livekit.cloud`
- `LIVEKIT_API_KEY`
- `LIVEKIT_API_SECRET`

These credentials must remain server-side. The Edge Function issues short-lived, participant-specific room tokens.

## Incoming calls

Incoming calls are detected through Supabase polling while Line Call is open. The app checks for a new incoming call every 2.5 seconds and uses Supabase call status updates to manage ringing, acceptance, decline, and expiry. This does not guarantee ringing when Android suspends or kills the app. Production-grade background incoming calls require push notifications (for example FCM) and Android call-notification handling. No Firebase project, Firebase SDK, or Firebase service-account key is currently configured.

## User flow

1. User enters an email address (no account is created yet).
2. User creates and confirms a password (still no account is created).
3. User enters an international phone number and presses Complete.
4. The server creates the Auth user and profile together, then returns a signed-in session. No email confirmation or SMS verification is used.
5. Users can sign in using either their email address or registered international phone number and password.
6. Users call other registered Line Call users. The recipient must have Line Call installed, be signed in, and have the app open to receive the current polling-based incoming call alert.
7. The recipient accepts or declines; accepted calls join a private LiveKit room.

## Build

Open the project in Android Studio or Google AI Studio. The GitHub Actions workflow also runs:

```bash
gradle :app:assembleDebug
```

## Important limitations

- A recipient must have a Line Call account and internet access.
- Incoming calls are discovered while the recipient app is open. Reliable alerts after Android has suspended or closed the app are not available in this Supabase-only configuration.
- Without the three LiveKit secrets, call invitations work but audio rooms cannot connect.
- Phone-number ownership is not verified. Users should only register numbers they control.
- This release is voice-only; it does not connect to the public telephone network or call ordinary GSM numbers.
