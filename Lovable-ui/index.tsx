import { createFileRoute, Link } from "@tanstack/react-router";
import { PhoneIncoming, PhoneOutgoing, PhoneMissed, Phone, Video, PhoneCall } from "lucide-react";
import { useState } from "react";
import { Shell } from "@/components/app/Shell";
import { Avatar } from "@/components/app/Avatar";
import { calls, getContact, useContacts } from "@/lib/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/")({
  head: () => ({ meta: [
    { title: "Recents — Line Call" },
    { name: "description", content: "Your recent incoming, outgoing and missed Line Call internet calls." },
    { property: "og:title", content: "Recents — Line Call" },
    { property: "og:description", content: "Your recent incoming, outgoing and missed Line Call internet calls." },
  ] }),
  component: Recents,
});

const icons = { incoming: PhoneIncoming, outgoing: PhoneOutgoing, missed: PhoneMissed };

function Recents() {
  useContacts();
  const [filter, setFilter] = useState<"all" | "missed">("all");
  const list = calls.filter((c) => filter === "all" || c.type === "missed");
  return (
    <Shell title="Recents" action={
      <Link to="/incoming/$id" params={{ id: "8" }} className="tap mb-1.5 flex items-center gap-1.5 rounded-full bg-secondary px-3 py-1.5 text-xs font-medium">
        <PhoneCall className="h-3.5 w-3.5" /> Demo call
      </Link>}>
      <div className="mx-5 mb-3 inline-flex rounded-full bg-secondary p-1">
        {(["all", "missed"] as const).map((f) => (
          <button key={f} onClick={() => setFilter(f)} className={cn("tap rounded-full px-5 py-1.5 text-sm font-medium capitalize transition-all", filter === f ? "bg-background shadow-sm" : "text-muted-foreground")}>{f}</button>
        ))}
      </div>
      <ul>
        {list.map((call, i) => {
          const c = getContact(call.contactId)!;
          const Icon = icons[call.type];
          return (
            <li key={call.id} className="animate-in fade-in slide-in-from-bottom-1 fill-mode-both" style={{ animationDelay: `${i * 35}ms` }}>
              <div className="flex items-center gap-3.5 px-5 py-2.5 active:bg-secondary/70">
                <Avatar contact={c} />
                <div className="min-w-0 flex-1 border-b border-border pb-2.5 pt-1">
                  <p className={cn("truncate font-semibold", call.type === "missed" && "text-destructive")}>{c.name}</p>
                  <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
                    <Icon className={cn("h-3.5 w-3.5", call.type === "missed" ? "text-destructive" : "text-primary")} />
                    {call.type.charAt(0).toUpperCase() + call.type.slice(1)}{call.duration && ` · ${call.duration}`} · {call.time}
                  </p>
                </div>
                <Link to="/call/$id" params={{ id: c.id }} aria-label={`Call ${c.name}`} className="tap flex h-10 w-10 items-center justify-center rounded-full bg-accent text-accent-foreground">
                  {call.video ? <Video className="h-[18px] w-[18px]" /> : <Phone className="h-[18px] w-[18px]" />}
                </Link>
              </div>
            </li>
          );
        })}
      </ul>
    </Shell>
  );
}
