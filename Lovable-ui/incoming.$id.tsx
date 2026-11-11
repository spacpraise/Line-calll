import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Phone, PhoneOff, MessageSquare, Lock } from "lucide-react";
import { Avatar } from "@/components/app/Avatar";
import { getContact } from "@/lib/store";

export const Route = createFileRoute("/incoming/$id")({
  head: () => ({ meta: [
    { title: "Incoming call — Line Call" },
    { name: "description", content: "Answer or decline an incoming Line Call." },
    { property: "og:title", content: "Incoming call — Line Call" },
    { property: "og:description", content: "Answer or decline an incoming Line Call." },
  ] }),
  component: Incoming,
});

function Incoming() {
  const { id } = Route.useParams();
  const c = getContact(id) ?? getContact("1")!;
  const nav = useNavigate();
  return (
    <div className="call-screen">
      <div className="flex flex-1 flex-col items-center pt-24 animate-in fade-in duration-500">
        <p className="mb-2 flex items-center gap-1.5 text-sm text-call-foreground/70"><Lock className="h-3 w-3" />Line Call audio</p>
        <h1 className="text-4xl font-semibold">{c.name}</h1>
        <p className="mt-2 text-call-foreground/60">{c.phone}</p>
        <div className="relative mt-16">
          <span className="ring-pulse" /><span className="ring-pulse [animation-delay:0.8s]" />
          <Avatar contact={c} size={140} badge={false} className="relative" />
        </div>
      </div>
      <div className="px-10 pb-14">
        <button className="tap mx-auto mb-10 flex items-center gap-2 rounded-full bg-call-foreground/12 px-4 py-2 text-sm"><MessageSquare className="h-4 w-4" />Reply with message</button>
        <div className="flex justify-between">
          <div className="flex flex-col items-center gap-2">
            <button onClick={() => nav({ to: "/" })} aria-label="Decline" className="tap flex h-[72px] w-[72px] items-center justify-center rounded-full bg-destructive text-destructive-foreground"><PhoneOff className="h-8 w-8" /></button>
            <span className="text-sm text-call-foreground/75">Decline</span>
          </div>
          <div className="flex flex-col items-center gap-2">
            <button onClick={() => nav({ to: "/call/$id", params: { id: c.id } })} aria-label="Answer" className="tap answer-btn flex h-[72px] w-[72px] items-center justify-center rounded-full bg-primary text-primary-foreground"><Phone className="h-8 w-8 fill-current" /></button>
            <span className="text-sm text-call-foreground/75">Answer</span>
          </div>
        </div>
      </div>
    </div>
  );
}
