import { createClient } from "npm:@supabase/supabase-js@2";
import { AccessToken } from "npm:livekit-server-sdk@2.13.0";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });

const isE164 = (value: unknown): value is string =>
  typeof value === "string" && /^\+[1-9][0-9]{6,14}$/.test(value);

const base64Url = (bytes: Uint8Array): string => {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
};

async function getFirebaseAccessToken(serviceAccount: Record<string, string>): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(new TextEncoder().encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const claims = base64Url(new TextEncoder().encode(JSON.stringify({
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: serviceAccount.token_uri || "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  })));
  const unsigned = header + "." + claims;
  const pem = serviceAccount.private_key.replace(/\\n/g, "\n").replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----/g, "").replace(/\s/g, "");
  const der = Uint8Array.from(atob(pem), (character) => character.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    "pkcs8",
    der,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = new Uint8Array(await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned),
  ));
  const assertion = unsigned + "." + base64Url(signature);
  const tokenResponse = await fetch(serviceAccount.token_uri || "https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const tokenBody = await tokenResponse.json();
  if (!tokenResponse.ok || !tokenBody.access_token) {
    throw new Error("Firebase OAuth token request failed: " + JSON.stringify(tokenBody));
  }
  return tokenBody.access_token as string;
}

async function sendIncomingCallPush(
  deviceToken: string,
  call: { id: string; caller_phone: string; caller_name: string },
): Promise<void> {
  const rawAccount = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON");
  if (!rawAccount) throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON secret is missing");
  const serviceAccount = JSON.parse(rawAccount) as Record<string, string>;
  const projectId = serviceAccount.project_id;
  if (!projectId || !serviceAccount.private_key || !serviceAccount.client_email) {
    throw new Error("Firebase service-account JSON is incomplete");
  }
  const accessToken = await getFirebaseAccessToken(serviceAccount);
  const response = await fetch(
    "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send",
    {
      method: "POST",
      headers: {
        "Authorization": "Bearer " + accessToken,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token: deviceToken,
          data: {
            type: "INCOMING_CALL",
            call_id: call.id,
            caller_phone: call.caller_phone,
            caller_name: call.caller_name || call.caller_phone,
          },
          android: {
            priority: "HIGH",
            ttl: "45s",
            collapse_key: "linecall-incoming",
          },
        },
      }),
    },
  );
  if (!response.ok) {
    const detail = await response.text();
    throw new Error("FCM send failed (" + response.status + "): " + detail);
  }
}



async function sendCallEndedPush(deviceToken: string, callId: string): Promise<void> {
  const rawAccount = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON");
  if (!rawAccount) throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON secret is missing");
  const serviceAccount = JSON.parse(rawAccount) as Record<string, string>;
  const projectId = serviceAccount.project_id;
  if (!projectId || !serviceAccount.private_key || !serviceAccount.client_email) {
    throw new Error("Firebase service-account JSON is incomplete");
  }
  const accessToken = await getFirebaseAccessToken(serviceAccount);
  const response = await fetch(
    "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send",
    {
      method: "POST",
      headers: {
        "Authorization": "Bearer " + accessToken,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token: deviceToken,
          data: {
            type: "CALL_ENDED",
            call_id: callId,
          },
          android: {
            priority: "HIGH",
            ttl: "30s",
            // Replace an undelivered incoming-call push with its cancellation.
            collapse_key: "linecall-incoming",
          },
        },
      }),
    },
  );
  if (!response.ok) {
    const detail = await response.text();
    throw new Error("FCM call-ended delivery failed (" + response.status + "): " + detail);
  }
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const authorization = req.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) return json({ error: "Sign in required" }, 401);

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !anonKey || !serviceKey) {
    return json({ error: "Supabase function environment is incomplete" }, 503);
  }

  const authClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authorization } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: { user }, error: authError } = await authClient.auth.getUser();
  if (authError || !user) return json({ error: "Invalid or expired session" }, 401);

  const db = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON body" }, 400);
  }

  const action = body.action;
  const callId = typeof body.call_id === "string" ? body.call_id : "";

  try {
    if (action === "lookup" || action === "initiate") {
      const phone = body.phone_number;
      if (!isE164(phone)) return json({ error: "Enter a valid international phone number." }, 400);

      const { data: recipient, error: lookupError } = await db
        .from("profiles")
        .select("id,email,phone_number,display_name,fcm_token")
        .eq("phone_number", phone)
        .maybeSingle();
      if (lookupError) throw lookupError;
      if (!recipient || recipient.id === user.id) {
        return json({ registered: false, error: recipient?.id === user.id ? "You cannot call your own number." : "This number is not registered on Line Call." }, 404);
      }

      if (action === "lookup") {
        return json({
          registered: true,
          user: { id: recipient.id, display_name: recipient.display_name, phone_number: recipient.phone_number },
        });
      }

      // Accepted calls renew a short lease while both clients remain connected.
      // If the app/process dies, a new call can clear the abandoned call after 3 minutes.
      const staleLeaseBefore = new Date(Date.now() - 180_000).toISOString();
      const { error: staleHeartbeatCleanupError } = await db
        .from("calls")
        .update({ status: "ended", ended_at: new Date().toISOString() })
        .eq("status", "accepted")
        .eq("heartbeat_enabled", true)
        .lt("last_activity_at", staleLeaseBefore);
      if (staleHeartbeatCleanupError) throw staleHeartbeatCleanupError;

      // Older app versions do not send heartbeats, so keep their calls compatible
      // and only clear abandoned legacy calls after 12 hours.
      const staleLegacyBefore = new Date(Date.now() - 12 * 60 * 60 * 1000).toISOString();
      const { error: staleLegacyCleanupError } = await db
        .from("calls")
        .update({ status: "ended", ended_at: new Date().toISOString() })
        .eq("status", "accepted")
        .eq("heartbeat_enabled", false)
        .lt("accepted_at", staleLegacyBefore);
      if (staleLegacyCleanupError) throw staleLegacyCleanupError;

      const { data: recipientActiveCalls, error: recipientActiveError } = await db
        .from("calls")
        .select("id")
        .in("status", ["ringing", "accepted"])
        .or(`caller_id.eq.${recipient.id},recipient_id.eq.${recipient.id}`)
        .limit(1);
      if (recipientActiveError) throw recipientActiveError;
      if (recipientActiveCalls?.length) {
        return json({ error: "This user is currently on another call." }, 409);
      }

      const { data: activeCalls, error: activeError } = await db
        .from("calls")
        .select("id")
        .in("status", ["ringing", "accepted"])
        .or(`caller_id.eq.${user.id},recipient_id.eq.${user.id}`)
        .limit(1);
      if (activeError) throw activeError;
      if (activeCalls?.length) return json({ error: "You already have an active call." }, 409);

      const since = new Date(Date.now() - 60_000).toISOString();
      const { count: recentCallCount, error: rateError } = await db
        .from("calls")
        .select("id", { count: "exact", head: true })
        .eq("caller_id", user.id)
        .gte("created_at", since);
      if (rateError) throw rateError;
      if ((recentCallCount ?? 0) >= 5) {
        return json({ error: "You have made several calls recently. Please wait a minute and try again." }, 429);
      }

      const { data: caller, error: callerError } = await db
        .from("profiles")
        .select("phone_number,display_name")
        .eq("id", user.id)
        .maybeSingle();
      if (callerError) throw callerError;
      if (!caller?.phone_number) return json({ error: "Add your phone number in your profile before calling." }, 400);

      const roomName = `linecall-${crypto.randomUUID()}`;
      const { data: call, error: insertError } = await db
        .from("calls")
        .insert({
          caller_id: user.id,
          recipient_id: recipient.id,
          caller_phone: caller.phone_number,
          recipient_phone: recipient.phone_number,
          room_name: roomName,
          status: "ringing",
        })
        .select("id,room_name,status,created_at,caller_phone,recipient_phone")
        .single();
      if (insertError) throw insertError;

      let pushSent = false;
      if (recipient.fcm_token) {
        try {
          await sendIncomingCallPush(recipient.fcm_token, {
            id: call.id,
            caller_phone: caller.phone_number,
            caller_name: caller.display_name ?? caller.phone_number,
          });
          pushSent = true;
        } catch (pushError) {
          console.error("Incoming-call FCM delivery failed", pushError);
        }
      }

      return json({
        call_id: call.id,
        push_sent: pushSent,
        room_name: call.room_name,
        status: call.status,
        phone_number: recipient.phone_number,
        display_name: recipient.display_name,
      });
    }

    if (action === "poll") {
      const { data: incoming, error } = await db
        .from("calls")
        .select("id,caller_id,caller_phone,created_at,status")
        .eq("recipient_id", user.id)
        .eq("status", "ringing")
        .order("created_at", { ascending: false })
        .limit(1)
        .maybeSingle();
      if (error) throw error;
      if (!incoming) return json({ incoming_call: null });

      const ageSeconds = (Date.now() - new Date(incoming.created_at).getTime()) / 1000;
      if (ageSeconds > 45) {
        await db.from("calls").update({ status: "missed", ended_at: new Date().toISOString() }).eq("id", incoming.id).eq("status", "ringing");
        return json({ incoming_call: null });
      }

      const { data: caller } = await db
        .from("profiles")
        .select("display_name")
        .eq("id", incoming.caller_id)
        .maybeSingle();

      return json({
        incoming_call: {
          call_id: incoming.id,
          phone_number: incoming.caller_phone,
          display_name: caller?.display_name ?? null,
          created_at: incoming.created_at,
        },
      });
    }

    if (!callId) return json({ error: "call_id is required" }, 400);
    const { data: call, error: callError } = await db
      .from("calls")
      .select("*")
      .eq("id", callId)
      .maybeSingle();
    if (callError) throw callError;
    if (!call || (call.caller_id !== user.id && call.recipient_id !== user.id)) {
      return json({ error: "Call not found" }, 404);
    }

    if (action === "heartbeat") {
      const { data, error } = await authClient.rpc("transition_line_call", { p_call_id: callId, p_action: "heartbeat", p_device_id: null });
      if (error) throw error;
      return json(data);
    }

    if (action === "status") {
      let status = call.status;
      if (status === "ringing" && (Date.now() - new Date(call.created_at).getTime()) / 1000 > 45) {
        await db.from("calls").update({ status: "missed", ended_at: new Date().toISOString() }).eq("id", callId).eq("status", "ringing");
        status = "missed";
      }
      return json({ call: { id: call.id, status, accepted_at: call.accepted_at, duration_seconds: call.duration_seconds } });
    }

    if (action === "accept") {
      if (call.recipient_id !== user.id) return json({ error: "Only the recipient can accept this call." }, 403);
      const deviceId = typeof body.device_id === "string" ? body.device_id : "";
      if (deviceId.length < 8 || deviceId.length > 128) return json({ error: "A valid device ID is required to answer." }, 400);
      const { data, error } = await authClient.rpc("transition_line_call", { p_call_id: callId, p_action: "accept", p_device_id: deviceId });
      if (error) throw error;
      if (!data?.accepted) return json({ error: "This call is no longer available.", call: data }, 409);
      if (data?.transitioned) {
        const { data: recipientProfile } = await db.from("profiles").select("fcm_token").eq("id", call.recipient_id).maybeSingle();
        if (recipientProfile?.fcm_token) {
          try { await sendCallEndedPush(recipientProfile.fcm_token, callId); }
          catch (pushError) { console.error("Could not clear answered call notification", pushError); }
        }
      }
      return json({ accepted: true, call_id: callId, status: data.status, idempotent: !!data.idempotent });
    }

    if (action === "decline") {
      if (call.recipient_id !== user.id) return json({ error: "Only the recipient can decline this call." }, 403);
      const { data, error } = await authClient.rpc("transition_line_call", { p_call_id: callId, p_action: "decline", p_device_id: null });
      if (error) throw error;
      return json({ declined: !!data?.declined, status: data?.status, transitioned: !!data?.transitioned });
    }

    if (action === "end") {
      const { data, error } = await authClient.rpc("transition_line_call", { p_call_id: callId, p_action: "end", p_device_id: null });
      if (error) throw error;
      if (data?.transitioned) {
        const otherUserId = call.caller_id === user.id ? call.recipient_id : call.caller_id;
        const { data: otherProfile, error: profileError } = await db.from("profiles").select("fcm_token").eq("id", otherUserId).maybeSingle();
        if (profileError) console.error("Could not load other participant FCM token", profileError);
        else if (otherProfile?.fcm_token) {
          try { await sendCallEndedPush(otherProfile.fcm_token, callId); }
          catch (pushError) { console.error("Call-ended FCM delivery failed", pushError); }
        }
      }
      return json({ ended: !!data?.ended, status: data?.status, duration_seconds: data?.duration_seconds });
    }

    if (action === "token") {
      if (call.status !== "accepted") return json({ error: "The other person has not accepted the call yet." }, 409);
      const livekitUrl = Deno.env.get("LIVEKIT_URL");
      const livekitKey = Deno.env.get("LIVEKIT_API_KEY");
      const livekitSecret = Deno.env.get("LIVEKIT_API_SECRET");
      if (!livekitUrl || !livekitKey || !livekitSecret) {
        return json({ error: "LiveKit is not configured. Add LIVEKIT_URL, LIVEKIT_API_KEY and LIVEKIT_API_SECRET to Supabase Edge Function secrets." }, 503);
      }
      const token = new AccessToken(livekitKey, livekitSecret, {
        identity: user.id,
        name: typeof body.participant_name === "string" ? body.participant_name : user.email ?? user.id,
        ttl: "1h",
      });
      token.addGrant({
        roomJoin: true,
        room: call.room_name,
        canPublish: true,
        canSubscribe: true,
      });
      return json({ livekit_url: livekitUrl, token: await token.toJwt() });
    }

    return json({ error: "Unsupported action" }, 400);
  } catch (error) {
    console.error("call-control error", error);
    return json({ error: error instanceof Error ? error.message : "Unexpected server error" }, 500);
  }
});
