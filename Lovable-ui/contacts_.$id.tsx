import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { ChevronLeft, Phone, Video, MessageCircle, Star, Mail, Smartphone, Share2, Ban, Trash2, PhoneIncoming, PhoneOutgoing, PhoneMissed, ShieldCheck, Cloud, Building2, Copy, Check } from "lucide-react";
import { useState, type ReactNode } from "react";
import { Avatar } from "@/components/app/Avatar";
import { EditSheet, type Draft } from "@/components/app/EditSheet";
import { calls, deleteContact, presenceLabel, toggleFavorite, useContacts } from "@/lib/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/contacts_/$id")({
  head: () => ({ meta: [
    { title: "Contact — Line Call" },
    { name: "description", content: "View, edit and call a contact on Line Call." },
    { property: "og:title", content: "Contact — Line Call" },
    { property: "og:description", content: "View, edit and call a contact on Line Call." },
  ] }),
  component: ContactPage,
});

const callIcon = { incoming: PhoneIncoming, outgoing: PhoneOutgoing, missed: PhoneMissed };

function Action({ icon, label, onClick, to, id, primary }: { icon: ReactNode; label: string; onClick?: () => void; to?: "/call/$id"; id?: string; primary?: boolean }) {
  const cls = cn("tap flex flex-1 flex-col items-center gap-1.5 rounded-2xl py-3 text-xs font-medium", primary ? "bg-primary text-primary-foreground call-btn" : "bg-secondary");
  return to && id ? <Link to={to} params={{ id }} className={cls}>{icon}{label}</Link> : <button onClick={onClick} className={cls}>{icon}{label}</button>;
}

function ContactPage() {
  const { id } = Route.useParams();
  const contacts = useContacts();
  const c = contacts.find((x) => x.id === id);
  const nav = useNavigate();
  const [edit, setEdit] = useState<Draft | null>(null);
  const [copied, setCopied] = useState(false);
  const [blocked, setBlocked] = useState(false);
  const [confirm, setConfirm] = useState(false);

  if (!c) return (
    <div className="app-frame items-center justify-center gap-4 p-8 text-center">
      <p className="text-muted-foreground">This contact no longer exists.</p>
      <Link to="/contacts" className="tap rounded-full bg-primary px-5 py-2 font-medium text-primary-foreground">Back to contacts</Link>
    </div>
  );
  const history = calls.filter((x) => x.contactId === c.id);
  const copy = () => { navigator.clipboard?.writeText(c.phone); setCopied(true); setTimeout(() => setCopied(false), 1500); };

  return (
    <div className="app-frame pb-10">
      <div className="profile-hero relative px-5 pb-6 pt-4">
        <div className="flex items-center justify-between">
          <Link to="/contacts" aria-label="Back" className="tap -ml-2 flex items-center gap-0.5 rounded-full p-2 font-medium"><ChevronLeft className="h-6 w-6" />Contacts</Link>
          <button onClick={() => setEdit({ id: c.id, name: c.name, phone: c.phone, email: c.email ?? "" })} className="tap rounded-full bg-background px-4 py-1.5 text-sm font-semibold shadow-sm">Edit</button>
        </div>
        <div className="mt-4 flex flex-col items-center text-center animate-in fade-in zoom-in-95 duration-300">
          <Avatar contact={c} size={112} />
          <h1 className="mt-4 text-2xl font-bold tracking-tight">{c.name}</h1>
          {c.company && <p className="text-sm text-muted-foreground">{c.company}</p>}
          <div className="mt-2 flex items-center gap-2">
            {c.registered ? (
              <span className="inline-flex items-center gap-1 rounded-full bg-accent px-2.5 py-1 text-xs font-semibold text-accent-foreground"><ShieldCheck className="h-3.5 w-3.5" />Line Call</span>
            ) : <span className="rounded-full bg-secondary px-2.5 py-1 text-xs font-medium text-muted-foreground">Not on Line Call</span>}
            {c.registered && <span className={cn("text-xs font-medium", c.presence === "online" ? "text-primary" : "text-muted-foreground")}>{presenceLabel(c)}</span>}
          </div>
        </div>
        <div className="mt-6 flex gap-2.5">
          <Action primary icon={<Phone className="h-5 w-5" />} label="Call" to="/call/$id" id={c.id} />
          <Action icon={<Video className="h-5 w-5" />} label="Video" to="/call/$id" id={c.id} />
          <Action icon={<MessageCircle className="h-5 w-5" />} label="Message" />
          <Action icon={<Star className={cn("h-5 w-5", c.favorite && "fill-primary text-primary")} />} label={c.favorite ? "Favourite" : "Favourite"} onClick={() => toggleFavorite(c.id)} />
        </div>
      </div>

      <div className="space-y-5 px-4">
        <section className="divide-y divide-border overflow-hidden rounded-3xl border border-border">
          <InfoRow icon={<Smartphone className="h-4 w-4" />} label="Mobile" value={c.phone}
            right={<button onClick={copy} aria-label="Copy number" className="tap rounded-full p-2 text-muted-foreground">{copied ? <Check className="h-4 w-4 text-primary" /> : <Copy className="h-4 w-4" />}</button>} />
          {c.email && <InfoRow icon={<Mail className="h-4 w-4" />} label="Email" value={c.email} />}
          {c.company && <InfoRow icon={<Building2 className="h-4 w-4" />} label="Company" value={c.company} />}
          <InfoRow icon={<Cloud className="h-4 w-4" />} label="Saved in" value={c.source === "google" ? "Google Contacts · synced" : "Phone"} />
        </section>

        {!c.registered && (
          <div className="flex items-center gap-3 rounded-3xl bg-foreground p-4 text-background">
            <div className="flex-1"><p className="font-semibold">Invite {c.name.split(" ")[0]}</p><p className="text-xs opacity-70">Free HD calls once they join Line Call.</p></div>
            <button className="tap rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground">Invite</button>
          </div>
        )}

        <section>
          <h2 className="mb-2 px-1 text-xs font-semibold uppercase tracking-wider text-muted-foreground">Call history</h2>
          <div className="divide-y divide-border overflow-hidden rounded-3xl border border-border">
            {history.length === 0 && <p className="p-4 text-sm text-muted-foreground">No calls yet</p>}
            {history.map((h) => { const I = callIcon[h.type]; return (
              <div key={h.id} className="flex items-center gap-3 px-4 py-3">
                <I className={cn("h-4 w-4", h.type === "missed" ? "text-destructive" : "text-primary")} />
                <span className="flex-1 text-sm font-medium capitalize">{h.type}{h.video && " video"} call</span>
                <span className="text-sm text-muted-foreground">{h.duration ?? "—"} · {h.time}</span>
              </div>); })}
          </div>
        </section>

        <section className="divide-y divide-border overflow-hidden rounded-3xl border border-border">
          <button className="tap flex w-full items-center gap-3 px-4 py-3.5 text-left font-medium"><Share2 className="h-4 w-4" />Share contact</button>
          <button onClick={() => setBlocked(!blocked)} className="tap flex w-full items-center gap-3 px-4 py-3.5 text-left font-medium text-destructive"><Ban className="h-4 w-4" />{blocked ? "Unblock contact" : "Block contact"}</button>
          <button onClick={() => setConfirm(true)} className="tap flex w-full items-center gap-3 px-4 py-3.5 text-left font-medium text-destructive"><Trash2 className="h-4 w-4" />Delete contact</button>
        </section>
      </div>

      {edit && <EditSheet draft={edit} onClose={() => setEdit(null)} />}
      {confirm && (
        <div className="fixed inset-0 z-30 flex items-end justify-center bg-foreground/40 animate-in fade-in" onClick={() => setConfirm(false)}>
          <div onClick={(e) => e.stopPropagation()} className="w-full max-w-[480px] rounded-t-[28px] bg-background p-5 pb-8 text-center animate-in slide-in-from-bottom duration-300">
            <p className="text-lg font-semibold">Delete {c.name}?</p>
            <p className="mt-1 text-sm text-muted-foreground">This removes them from your phone and Google Contacts.</p>
            <button onClick={() => { deleteContact(c.id); nav({ to: "/contacts" }); }} className="tap mt-5 w-full rounded-2xl bg-destructive py-3.5 font-semibold text-destructive-foreground">Delete contact</button>
            <button onClick={() => setConfirm(false)} className="tap mt-2 w-full rounded-2xl bg-secondary py-3.5 font-semibold">Cancel</button>
          </div>
        </div>
      )}
    </div>
  );
}

function InfoRow({ icon, label, value, right }: { icon: ReactNode; label: string; value: string; right?: ReactNode }) {
  return (
    <div className="flex items-center gap-3.5 px-4 py-3">
      <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-secondary">{icon}</span>
      <div className="min-w-0 flex-1"><p className="text-xs text-muted-foreground">{label}</p><p className="truncate text-[15px] font-medium">{value}</p></div>
      {right}
    </div>
  );
}
