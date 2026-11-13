import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Phone, Delete } from "lucide-react";
import { useState } from "react";
import { Shell } from "@/components/app/Shell";
import { Avatar } from "@/components/app/Avatar";
import { useContacts } from "@/lib/store";

export const Route = createFileRoute("/dialer")({
  head: () => ({ meta: [
    { title: "Keypad — Line Call" },
    { name: "description", content: "Dial any number and call over the internet with Line Call." },
    { property: "og:title", content: "Keypad — Line Call" },
    { property: "og:description", content: "Dial any number and call over the internet with Line Call." },
  ] }),
  component: Dialer,
});

const keys = [["1", ""], ["2", "ABC"], ["3", "DEF"], ["4", "GHI"], ["5", "JKL"], ["6", "MNO"], ["7", "PQRS"], ["8", "TUV"], ["9", "WXYZ"], ["*", ""], ["0", "+"], ["#", ""]];

function Dialer() {
  const [num, setNum] = useState("");
  const contacts = useContacts();
  const nav = useNavigate();
  const digits = num.replace(/\D/g, "");
  const matches = digits.length >= 2 ? contacts.filter((c) => c.phone.replace(/\D/g, "").includes(digits)).slice(0, 2) : [];
  const call = () => {
    const id = matches[0]?.id ?? contacts[0]?.id ?? "1";
    if (num) nav({ to: "/call/$id", params: { id } });
  };
  return (
    <Shell title="Keypad">
      <div className="flex flex-col items-center px-6">
        <div className="flex h-24 w-full items-center justify-center">
          <p className="truncate text-4xl font-light tracking-wider">{num || <span className="text-muted-foreground/50">Enter number</span>}</p>
        </div>
        <div className="mb-4 h-14 w-full">
          {matches.map((c) => (
            <button key={c.id} onClick={() => setNum(c.phone)} className="tap flex w-full items-center gap-3 rounded-2xl px-3 py-1.5 animate-in fade-in hover:bg-secondary">
              <Avatar contact={c} size={34} />
              <span className="text-sm font-semibold">{c.name}</span>
              <span className="ml-auto text-xs text-muted-foreground">{c.phone}</span>
            </button>
          ))}
        </div>
        <div className="grid grid-cols-3 gap-x-7 gap-y-4">
          {keys.map(([k, sub]) => (
            <button key={k} onClick={() => setNum((n) => n + k)} onContextMenu={(e) => { if (k === "0") { e.preventDefault(); setNum((n) => n + "+"); } }}
              className="key flex h-[76px] w-[76px] flex-col items-center justify-center rounded-full bg-secondary">
              <span className="text-[30px] leading-none">{k}</span>
              <span className="mt-0.5 h-3 text-[9px] font-semibold tracking-[0.2em] text-muted-foreground">{sub}</span>
            </button>
          ))}
        </div>
        <div className="mt-6 grid w-full grid-cols-3 items-center justify-items-center">
          <span />
          <button onClick={call} aria-label="Call" className="tap call-btn flex h-[76px] w-[76px] items-center justify-center rounded-full bg-primary text-primary-foreground">
            <Phone className="h-8 w-8 fill-current" />
          </button>
          {num && <button onClick={() => setNum((n) => n.slice(0, -1))} aria-label="Delete" className="tap p-3 text-muted-foreground animate-in fade-in"><Delete className="h-7 w-7" /></button>}
        </div>
      </div>
    </Shell>
  );
}
