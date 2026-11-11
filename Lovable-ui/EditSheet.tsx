import { X, Pencil } from "lucide-react";
import { useState } from "react";
import { getContact, saveContact, type Contact } from "@/lib/store";

export type Draft = { id?: string; name: string; phone: string; email: string };

export function EditSheet({ draft, onClose }: { draft: Draft; onClose: () => void }) {
  const [d, setD] = useState(draft);
  const valid = d.name.trim() && d.phone.trim();
  const save = () => {
    if (!valid) return;
    const base = { name: d.name.trim(), phone: d.phone.trim(), email: d.email };
    if (d.id) saveContact({ ...(getContact(d.id) as Contact), ...base });
    else saveContact({ ...base, registered: false, source: "phone" });
    onClose();
  };
  return (
    <div className="fixed inset-0 z-30 flex items-end justify-center bg-foreground/40 animate-in fade-in" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="w-full max-w-[480px] rounded-t-[28px] bg-background p-5 pb-8 animate-in slide-in-from-bottom duration-300">
        <div className="mx-auto mb-4 h-1 w-10 rounded-full bg-border" />
        <div className="mb-4 flex items-center justify-between">
          <button onClick={onClose} aria-label="Close" className="tap rounded-full p-2 hover:bg-secondary"><X className="h-5 w-5" /></button>
          <h2 className="flex items-center gap-2 text-lg font-semibold">{d.id ? <><Pencil className="h-4 w-4" />Edit contact</> : "New contact"}</h2>
          <button onClick={save} disabled={!valid} className="tap rounded-full bg-primary px-4 py-1.5 text-sm font-semibold text-primary-foreground disabled:opacity-40">Save</button>
        </div>
        {(["name", "phone", "email"] as const).map((f) => (
          <input key={f} value={d[f]} onChange={(e) => setD({ ...d, [f]: e.target.value })} placeholder={f === "name" ? "Full name" : f === "phone" ? "Phone number" : "Email (optional)"}
            inputMode={f === "phone" ? "tel" : undefined} className="mb-3 w-full rounded-2xl bg-secondary px-4 py-3.5 text-[15px] outline-none ring-primary focus:ring-2" />
        ))}
        <p className="text-xs text-muted-foreground">Saved to your phone and synced with Google Contacts.</p>
      </div>
    </div>
  );
}
