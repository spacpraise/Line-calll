import { createFileRoute } from "@tanstack/react-router";
import { ChevronRight, RefreshCw, Mic, Users, Bell, Moon, Wifi, Shield, HelpCircle, LogOut, Contact } from "lucide-react";
import { useState, type ReactNode } from "react";
import { Shell } from "@/components/app/Shell";
import { Avatar } from "@/components/app/Avatar";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/settings")({
  head: () => ({ meta: [
    { title: "Profile & Settings — Line Call" },
    { name: "description", content: "Manage your Line Call account, contact syncing, permissions and preferences." },
    { property: "og:title", content: "Profile & Settings — Line Call" },
    { property: "og:description", content: "Manage your Line Call account, contact syncing, permissions and preferences." },
  ] }),
  component: Settings,
});

function Toggle({ on, set }: { on: boolean; set: (v: boolean) => void }) {
  return (
    <button role="switch" aria-checked={on} onClick={() => set(!on)} className={cn("relative h-7 w-12 rounded-full transition-colors", on ? "bg-primary" : "bg-muted")}>
      <span className={cn("absolute top-0.5 h-6 w-6 rounded-full bg-background shadow transition-transform duration-200", on ? "translate-x-[22px]" : "translate-x-0.5")} />
    </button>
  );
}
function Row({ icon, label, hint, right }: { icon: ReactNode; label: string; hint?: string; right?: ReactNode }) {
  return (
    <div className="flex items-center gap-3.5 px-4 py-3 active:bg-secondary/70">
      <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-secondary">{icon}</span>
      <div className="flex-1"><p className="text-[15px] font-medium">{label}</p>{hint && <p className="text-xs text-muted-foreground">{hint}</p>}</div>
      {right ?? <ChevronRight className="h-4 w-4 text-muted-foreground" />}
    </div>
  );
}
function Group({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mb-6">
      <h2 className="mb-2 px-5 text-xs font-semibold uppercase tracking-wider text-muted-foreground">{title}</h2>
      <div className="mx-4 divide-y divide-border overflow-hidden rounded-3xl border border-border">{children}</div>
    </section>
  );
}

function Settings() {
  const [s, setS] = useState({ google: true, phone: true, mic: true, contacts: true, notif: true, hd: true, dataSaver: false, presence: true });
  const t = (k: keyof typeof s) => <Toggle on={s[k]} set={(v) => setS({ ...s, [k]: v })} />;
  return (
    <Shell title="Profile">
      <div className="mx-4 mb-6 flex items-center gap-4 rounded-3xl bg-foreground p-5 text-background">
        <Avatar contact={{ name: "Virami Ade", registered: true, presence: "online" }} size={64} badge={false} />
        <div className="flex-1">
          <p className="text-lg font-semibold">Virami Ade</p>
          <p className="text-sm opacity-70">+234 810 555 2210</p>
          <p className="mt-1 inline-flex items-center gap-1.5 text-xs font-medium text-primary"><span className="h-2 w-2 rounded-full bg-primary" />Online</p>
        </div>
        <button className="tap rounded-full bg-background/15 px-3 py-1.5 text-xs font-medium">Edit</button>
      </div>
      <Group title="Contact sync">
        <Row icon={<RefreshCw className="h-4 w-4" />} label="Google Contacts" hint="virami@gmail.com · synced 2 min ago" right={t("google")} />
        <Row icon={<Contact className="h-4 w-4" />} label="Phone contacts" hint="214 contacts" right={t("phone")} />
      </Group>
      <Group title="Permissions">
        <Row icon={<Mic className="h-4 w-4" />} label="Microphone" hint={s.mic ? "Allowed" : "Required for calls"} right={t("mic")} />
        <Row icon={<Users className="h-4 w-4" />} label="Contacts access" hint={s.contacts ? "Allowed" : "Denied"} right={t("contacts")} />
        <Row icon={<Bell className="h-4 w-4" />} label="Notifications" right={t("notif")} />
      </Group>
      <Group title="Preferences">
        <Row icon={<Wifi className="h-4 w-4" />} label="HD voice" hint="Best quality on Wi-Fi" right={t("hd")} />
        <Row icon={<Moon className="h-4 w-4" />} label="Low data mode" right={t("dataSaver")} />
        <Row icon={<Shield className="h-4 w-4" />} label="Show online status" right={t("presence")} />
        <Row icon={<HelpCircle className="h-4 w-4" />} label="Help & support" />
      </Group>
      <button className="tap mx-4 mb-4 flex w-[calc(100%-2rem)] items-center justify-center gap-2 rounded-3xl border border-border py-3.5 font-medium text-destructive"><LogOut className="h-4 w-4" />Sign out</button>
    </Shell>
  );
}
