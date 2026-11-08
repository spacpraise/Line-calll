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
        .select("id,email,phone_number,display_name")
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

      return json({
        call_id: call.id,
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
        await db.from("calls").update({ status: "missed", ended_at: new Date().toISOString() }).eq("id", incoming.id);
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

    if (action === "status") {
      let status = call.status;
      if (status === "ringing" && (Date.now() - new Date(call.created_at).getTime()) / 1000 > 45) {
        await db.from("calls").update({ status: "missed", ended_at: new Date().toISOString() }).eq("id", callId);
        status = "missed";
      }
      return json({ call: { id: call.id, status, accepted_at: call.accepted_at, duration_seconds: call.duration_seconds } });
    }

    if (action === "accept") {
      if (call.recipient_id !== user.id) return json({ error: "Only the recipient can accept this call." }, 403);
      if (call.status !== "ringing") return json({ error: "This call is no longer ringing." }, 409);
      if ((Date.now() - new Date(call.created_at).getTime()) / 1000 > 45) {
        await db.from("calls").update({ status: "missed", ended_at: new Date().toISOString() }).eq("id", callId);
        return json({ error: "This call has expired." }, 409);
      }
      const { error } = await db.from("calls").update({
        status: "accepted",
        accepted_at: new Date().toISOString(),
      }).eq("id", callId);
      if (error) throw error;
      return json({ accepted: true, call_id: callId });
    }

    if (action === "decline") {
      if (call.recipient_id !== user.id) return json({ error: "Only the recipient can decline this call." }, 403);
      if (call.status !== "ringing") return json({ declined: true });
      const { error } = await db.from("calls").update({
        status: "declined",
        ended_at: new Date().toISOString(),
      }).eq("id", callId);
      if (error) throw error;
      return json({ declined: true });
    }

    if (action === "end") {
      const now = new Date();
      const duration = call.accepted_at
        ? Math.max(0, Math.floor((now.getTime() - new Date(call.accepted_at).getTime()) / 1000))
        : 0;
      const { error } = await db.from("calls").update({
        status: "ended",
        ended_at: now.toISOString(),
        duration_seconds: duration,
      }).eq("id", callId);
      if (error) throw error;
      return json({ ended: true, duration_seconds: duration });
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
