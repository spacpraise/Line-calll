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
  if (!url || !anonKey || !serviceKey) return json({ error: "Account lookup is temporarily unavailable." }, 503);
  if (req.headers.get("apikey") !== anonKey) return json({ error: "Unauthorized" }, 401);

  let body: Record<string, unknown>;
  try { body = await req.json(); } catch { return json({ error: "Invalid request." }, 400); }
  const email = typeof body.email === "string" ? body.email.trim().toLowerCase() : "";
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return json({ error: "Enter a valid email address." }, 400);

  try {
    const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });
    const ip = req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || "unknown";
    const identifierHash = await sha256("email-check:" + email);
    const ipHash = await sha256(ip);
    const since = new Date(Date.now() - 60 * 60 * 1000).toISOString();

    const { count: emailCount, error: emailErr } = await admin.from("password_reset_attempts")
      .select("id", { count: "exact", head: true }).eq("identifier_hash", identifierHash).gte("created_at", since);
    const { count: ipCount, error: ipErr } = await admin.from("password_reset_attempts")
      .select("id", { count: "exact", head: true }).eq("ip_hash", ipHash).gte("created_at", since);
    if (emailErr || ipErr) throw new Error("Rate limit check failed");
    if ((emailCount ?? 0) >= 5 || (ipCount ?? 0) >= 20) return json({ error: "Too many attempts. Please try again later." }, 429);
    const { error: logError } = await admin.from("password_reset_attempts")
      .insert({ identifier_hash: identifierHash, ip_hash: ipHash });
    if (logError) throw logError;

    let matchedUser: { id: string } | null = null;
    for (let page = 1; page <= 50; page++) {
      const { data, error } = await admin.auth.admin.listUsers({ page, perPage: 1000 });
      if (error) throw error;
      const users = data.users ?? [];
      const match = users.find((user) => user.email?.toLowerCase() === email);
      if (match) { matchedUser = { id: match.id }; break; }
      if (users.length < 1000) break;
    }
    if (!matchedUser) return json({ exists: false });
    const { data: profile } = await admin.from("profiles")
      .select("display_name, avatar_url")
      .eq("id", matchedUser.id)
      .maybeSingle();
    return json({
      exists: true,
      display_name: profile?.display_name ?? "",
      avatar_url: profile?.avatar_url ?? null,
    });
  } catch (error) {
    console.error("check-account failed:", error instanceof Error ? error.message : "unknown");
    return json({ error: "We couldn't check this email. Please try again." }, 503);
  }
});