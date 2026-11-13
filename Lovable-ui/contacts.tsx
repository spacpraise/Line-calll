import { createFileRoute, Link } from "@tanstack/react-router";
import { Plus, Search, Phone, RefreshCw, Check } from "lucide-react";
import { EditSheet, type Draft } from "@/components/app/EditSheet";
import { useMemo, useState } from "react";
import { Shell } from "@/components/app/Shell";
import { Avatar } from "@/components/app/Avatar";
import { presenceLabel, useContacts } from "@/lib/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/contacts")({
  head: () => ({ meta: [
    { title: "Contacts — Line Call" },
    { name: "description", content: "Phone and Google contacts, with Line Call users and their online status." },
    { property: "og:title", content: "Contacts — Line Call" },
    { property: "og:description", content: "Phone and Google contacts, with Line Call users and their online status." },
  ] }),
  component: Contacts,
});


function Contacts() {
  const contacts = useContacts();
  const [q, setQ] = useState("");
  const [tab, setTab] = useState<"all" | "line">("all");
  const [syncing, setSyncing] = useState<"idle" | "busy" | "done">("idle");
  const [edit, setEdit] = useState<Draft | null>(null);
  const list = useMemo(() => contacts.filter((c) => (tab === "all" || c.registered) && (c.name + c.phone).toLowerCase().includes(q.toLowerCase())), [contacts, q, tab]);

  const sync = () => { setSyncing("busy"); setTimeout(() => setSyncing("done"), 1600); setTimeout(() => setSyncing("idle"), 3600); };

  return (
    <Shell title="Contacts" action={
      <button onClick={() => setEdit({ name: "", phone: "", email: "" })} aria-label="Add contact" className="tap mb-1.5 flex h-10 w-10 items-center justify-center rounded-full bg-primary text-primary-foreground"><Plus className="h-5 w-5" /></button>}>
      <div className="space-y-3 px-5">
        <label className="flex items-center gap-2 rounded-2xl bg-secondary px-4 py-2.5">
          <Search className="h-4 w-4 text-muted-foreground" />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search name or number" className="w-full bg-transparent text-[15px] outline-none placeholder:text-muted-foreground" />
        </label>
        <div className="flex items-center justify-between">
          <div className="inline-flex rounded-full bg-secondary p-1">
            {([["all", "All"], ["line", "On Line Call"]] as const).map(([k, l]) => (
              <button key={k} onClick={() => setTab(k)} className={cn("tap rounded-full px-4 py-1.5 text-sm font-medium", tab === k ? "bg-background shadow-sm" : "text-muted-foreground")}>{l}</button>
            ))}
          </div>
          <button onClick={sync} disabled={syncing === "busy"} className="tap flex items-center gap-1.5 text-sm font-medium text-primary">
            {syncing === "done" ? <Check className="h-4 w-4" /> : <RefreshCw className={cn("h-4 w-4", syncing === "busy" && "animate-spin")} />}
            {syncing === "busy" ? "Syncing…" : syncing === "done" ? "Synced" : "Google sync"}
          </button>
        </div>
      </div>
      <ul className="mt-2">
        {list.map((c) => (
          <li key={c.id} className="flex items-center gap-3.5 px-5 py-2.5 active:bg-secondary/70">
            <Avatar contact={c} size={46} />
            <Link to="/contacts/$id" params={{ id: c.id }} className="min-w-0 flex-1 text-left">
              <p className="flex items-center gap-2 truncate font-semibold">{c.name}
                {c.registered && <span className="rounded-full bg-accent px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-accent-foreground">Line</span>}
              </p>
              <p className={cn("truncate text-sm", c.presence === "online" ? "text-primary" : "text-muted-foreground")}>{c.registered ? presenceLabel(c) : c.phone}</p>
            </Link>
            {c.registered ? (
              <Link to="/call/$id" params={{ id: c.id }} aria-label={`Call ${c.name}`} className="tap flex h-10 w-10 items-center justify-center rounded-full bg-accent text-accent-foreground"><Phone className="h-[18px] w-[18px]" /></Link>
            ) : <button className="tap rounded-full border border-border px-3 py-1.5 text-xs font-medium">Invite</button>}
          </li>
        ))}
        {list.length === 0 && <p className="px-5 py-10 text-center text-muted-foreground">No contacts found</p>}
      </ul>
      {edit && <EditSheet draft={edit} onClose={() => setEdit(null)} />}
    </Shell>
  );
}

