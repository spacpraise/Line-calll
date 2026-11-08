import { createClient } from "npm:@supabase/supabase-js@2";

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
  typeof value === "string" && /^\\+[1-9][0-9]{6,14}$/.test(value);

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !anonKey || !serviceKey) {
    return json({ error: "Authentication service is temporarily unavailable." }, 503);
  }

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid request." }, 400);
  }

  const phone = body.phone_number;
  const password = typeof body.password === "string" ? body.password : "";
  if (!isE164(phone) || password.length < 1 || password.length > 256) {
    return json({ error: "Enter a valid phone number and password." }, 400);
  }

  try {
    // The service-role key stays on the server. The phone-to-email mapping is
    // never returned to the device.
    const admin = createClient(supabaseUrl, serviceKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: profile, error: profileError } = await admin
      .from("profiles")
      .select("email")
      .eq("phone_number", phone)
      .maybeSingle();

    if (profileError || !profile?.email) {
      return json({ error: "Invalid phone number or password." }, 401);
    }

    // Let Supabase Auth verify the password and issue the normal user session.
    // This keeps password hashing, lockout/rate limits and JWT issuance in GoTrue.
    const authResponse = await fetch(
      `${supabaseUrl}/auth/v1/token?grant_type=password`,
      {
        method: "POST",
        headers: {
          "apikey": anonKey,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ email: profile.email, password }),
      },
    );

    const session = await authResponse.json().catch(() => ({}));
    if (!authResponse.ok || !session.access_token || !session.refresh_token) {
      return json({ error: "Invalid phone number or password." }, 401);
    }

    return json(session);
  } catch (error) {
    console.error("phone-login failed", error instanceof Error ? error.message : "unknown");
    return json({ error: "Unable to sign in right now. Please try again." }, 503);
  }
});
