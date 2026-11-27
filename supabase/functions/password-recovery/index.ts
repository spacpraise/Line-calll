import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { ...corsHeaders, "Content-Type": "application/json" } });
const genericFailure = () => json({ error: "The email and phone number could not be matched. Check both details and try again." }, 400);
const sha256 = async (value: string) => {
  const bytes = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, "0")).join("");
};
const validE164 = (value: unknown): value is string =>
  typeof value === "string" && value.startsWith("+") && /^[1-9][0-9]{6,14}$/.test(value.slice(1));

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !anonKey || !serviceKey) return json({ error: "Password recovery is temporarily unavailable." }, 503);

  let body: Record<string, unknown>;
  try { body = await req.json(); } catch { return json({ error: "Invalid request." }, 400); }

  const email = typeof body.email === "string" ? body.email.trim().toLowerCase() : "";
  const phone = body.phone_number;
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || !validE164(phone)) return genericFailure();

  const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });
  try {
    const ip = (req.headers.get("cf-connecting-ip") || req.headers.get("x-forwarded-for") || "unknown").split(",")[0].trim();
    const identifierHash = await sha256(email + "|" + phone);
    const ipHash = await sha256(ip);

    // Store hashes only. Limit repeated attempts for this email/phone pair and source IP.
    await admin.from("password_reset_attempts").insert({ identifier_hash: identifierHash, ip_hash: ipHash });
    const since = new Date(Date.now() - 60 * 60 * 1000).toISOString();
    const [pairAttempts, ipAttempts] = await Promise.all([
      admin.from("password_reset_attempts").select("id", { count: "exact", head: true })
        .eq("identifier_hash", identifierHash).gte("created_at", since),
      admin.from("password_reset_attempts").select("id", { count: "exact", head: true })
        .eq("ip_hash", ipHash).gte("created_at", since),
    ]);
    if (pairAttempts.error || ipAttempts.error) return json({ error: "Password recovery is temporarily unavailable." }, 503);
    if ((pairAttempts.count ?? 0) > 5 || (ipAttempts.count ?? 0) > 20) {
      return json({ error: "Too many attempts. Please try again later." }, 429);
    }

    const { data: matches, error: profileError } = await admin
      .from("profiles").select("id").eq("email", email).eq("phone_number", phone).limit(2);
    if (profileError || !matches || matches.length !== 1) return genericFailure();
    const matchedUserId = matches[0].id as string;

    // Build-phase behavior: the account stays completely untouched here (no
    // deletion, no row changes) -- this only proves the submitted email and
    // phone belong to the SAME existing profile row. Once that is confirmed,
    // mint a short-lived recovery token for that exact user id with the
    // admin API and hand the raw token back to the app directly, instead of
    // emailing a link. The app immediately exchanges this token at
    // /auth/v1/verify (type=recovery), the same path the email link would
    // have used, landing the user on the existing "choose a new password"
    // screen with their existing account, contacts and call history intact.
    const { data: linkData, error: linkError } = await admin.auth.admin.generateLink({
      type: "recovery",
      email,
    });
    if (linkError || !linkData?.properties?.hashed_token) {
      console.error("password-recovery link generation failed", linkError?.message);
      return genericFailure();
    }
    // Defensive check: the generated link must belong to the same matched
    // profile row we just verified, never a different account that happens
    // to share an unrelated field.
    if (linkData.user?.id && linkData.user.id !== matchedUserId) {
      console.error("password-recovery matched-user mismatch", { matchedUserId, generatedFor: linkData.user.id });
      return genericFailure();
    }

    return json({ matched: true, token_hash: linkData.properties.hashed_token });
  } catch (error) {
    console.error("password-recovery failed", error instanceof Error ? error.message : "unknown");
    return json({ error: "Password recovery is temporarily unavailable." }, 503);
  }
});
