import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { ...corsHeaders, "Content-Type": "application/json" } });

const sha256 = async (value: string) => {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, "0")).join("");
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !anonKey || !serviceKey) return json({ error: "Registration is temporarily unavailable." }, 503);

  let body: Record<string, unknown>;
  try { body = await req.json(); } catch { return json({ error: "Invalid request." }, 400); }
  const email = typeof body.email === "string" ? body.email.trim().toLowerCase() : "";
  const password = typeof body.password === "string" ? body.password : "";
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || password.length < 8 || password.length > 256) {
    return json({ error: "Enter a valid email address and a password of at least 8 characters." }, 400);
  }

  try {
    const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });
    const ip = req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || "unknown";
    const identifierHash = await sha256("signup:" + email);
    const ipHash = await sha256(ip);
    const since = new Date(Date.now() - 60 * 60 * 1000).toISOString();

    const { count: emailCount, error: emailCountError } = await admin
      .from("password_reset_attempts").select("id", { count: "exact", head: true })
      .eq("identifier_hash", identifierHash).gte("created_at", since);
    const { count: ipCount, error: ipCountError } = await admin
      .from("password_reset_attempts").select("id", { count: "exact", head: true })
      .eq("ip_hash", ipHash).gte("created_at", since);
    if (emailCountError || ipCountError) throw new Error("Rate limit check failed");
    if ((emailCount ?? 0) >= 5 || (ipCount ?? 0) >= 20) return json({ error: "Too many registration attempts. Please try again later." }, 429);

    const { error: logError } = await admin.from("password_reset_attempts")
      .insert({ identifier_hash: identifierHash, ip_hash: ipHash });
    if (logError) throw logError;

    // Explicitly mark the address confirmed at account creation. This avoids
    // Supabase's project-level email-confirmation setting blocking app signup.
    const { data: created, error: createError } = await admin.auth.admin.createUser({
      email, password, email_confirm: true,
    });
    if (createError || !created.user) {
      const message = createError?.message ?? "Unable to create account.";
      if (/already|exists|registered/i.test(message)) return json({ error: "An account with this email already exists. Please log in." }, 409);
      console.error("register-account createUser failed:", message);
      return json({ error: "Could not create your account. Please try again." }, 400);
    }

    const signIn = await fetch(`${url}/auth/v1/token?grant_type=password`, {
      method: "POST",
      headers: { apikey: anonKey, "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
    const session = await signIn.json().catch(() => ({}));
    if (!signIn.ok || !session.access_token || !session.refresh_token) {
      // Avoid leaving an account created without a usable session.
      await admin.auth.admin.deleteUser(created.user.id);
      console.error("register-account sign-in failed:", signIn.status);
      return json({ error: "Your account was created but sign-in could not be completed. Please log in." }, 503);
    }
    return json({
      access_token: session.access_token, refresh_token: session.refresh_token,
      expires_in: session.expires_in, expires_at: session.expires_at, token_type: session.token_type,
    });
  } catch (error) {
    console.error("register-account failed:", error instanceof Error ? error.message : "unknown");
    return json({ error: "Registration is temporarily unavailable. Please try again." }, 503);
  }
});
