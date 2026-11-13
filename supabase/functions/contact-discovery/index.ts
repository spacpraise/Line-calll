import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { ...corsHeaders, "Content-Type": "application/json" } });

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const authorization = req.headers.get("Authorization");
  if (!url || !anonKey || !serviceKey || !authorization) return json({ error: "Authentication required." }, 401);

  try {
    const authClient = createClient(url, anonKey, {
      auth: { persistSession: false, autoRefreshToken: false },
      global: { headers: { Authorization: authorization } },
    });
    const { data: authData, error: authError } = await authClient.auth.getUser();
    if (authError || !authData.user) return json({ error: "Your session is invalid. Sign in again." }, 401);
    const userId = authData.user.id;
    const body = await req.json().catch(() => null);
    if (!body || typeof body !== "object") return json({ error: "Invalid request." }, 400);
    const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });

    if (body.action === "presence" || body.action === "offline") {
      const deviceId = typeof body.device_id === "string" ? body.device_id : "";
      if (deviceId.length < 8 || deviceId.length > 128) return json({ error: "Invalid device identifier." }, 400);
      if (body.action === "offline") {
        const { error } = await admin.from("user_presence_sessions").delete()
          .eq("user_id", userId).eq("device_id", deviceId);
        if (error) throw error;
        return json({ presence_updated: true, online: false });
      }
      const state = body.state === "in_call" ? "in_call" : "online";
      const { error } = await admin.from("user_presence_sessions").upsert({
        user_id: userId, device_id: deviceId, state,
        expires_at: new Date(Date.now() + 120_000).toISOString(),
        updated_at: new Date().toISOString(),
      }, { onConflict: "user_id,device_id" });
      if (error) throw error;
      return json({ presence_updated: true, expires_in_seconds: 120 });
    }

    if (body.action !== "lookup") return json({ error: "Unsupported action." }, 400);
    const phones = Array.isArray(body.phone_numbers)
      ? [...new Set(body.phone_numbers.filter((v: unknown) => typeof v === "string" && /^\\+[1-9][0-9]{6,14}$/.test(v)))]
      : [];
    const emails = Array.isArray(body.emails)
      ? [...new Set(body.emails.filter((v: unknown) => typeof v === "string" && /^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/.test(v)).map((v: string) => v.trim().toLowerCase()))]
      : [];
    if (phones.length + emails.length > 500) return json({ error: "Please sync contacts in batches of 500 or fewer." }, 400);
    if (!phones.length && !emails.length) return json({ contacts: [] });

    const matches = new Map<string, Record<string, unknown>>();
    if (phones.length) {
      const { data, error } = await admin.from("profiles").select("id,email,phone_number,display_name,avatar_url")
        .in("phone_number", phones);
      if (error) throw error;
      for (const row of data ?? []) matches.set(row.id, row);
    }
    if (emails.length) {
      const { data, error } = await admin.from("profiles").select("id,email,phone_number,display_name,avatar_url")
        .in("email", emails);
      if (error) throw error;
      for (const row of data ?? []) matches.set(row.id, row);
    }
    matches.delete(userId);
    const users = [...matches.values()];
    const ids = users.map((row) => String(row.id));
    const presenceByUser = new Map<string, string>();
    if (ids.length) {
      const { data, error } = await admin.from("user_presence_sessions").select("user_id,state")
        .in("user_id", ids).gt("expires_at", new Date().toISOString());
      if (error) throw error;
      for (const row of data ?? []) {
        if (row.state === "in_call") presenceByUser.set(row.user_id, "in_call");
        else if (!presenceByUser.has(row.user_id)) presenceByUser.set(row.user_id, "online");
      }
    }
    return json({ contacts: users.map((row) => ({
      user_id: row.id, email: row.email, phone_number: row.phone_number,
      display_name: row.display_name, avatar_url: row.avatar_url,
      registered: true, presence: presenceByUser.get(String(row.id)) ?? "offline",
    })) });
  } catch (error) {
    console.error("contact-discovery failed:", error);
    return json({ error: error instanceof Error ? error.message : "Contact sync is temporarily unavailable." }, 500);
  }
});
