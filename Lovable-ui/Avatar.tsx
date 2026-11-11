import { cn } from "@/lib/utils";
import type { Contact } from "@/lib/store";

export function Avatar({ contact, size = 48, badge = true, className }: { contact: Pick<Contact, "name" | "registered" | "presence">; size?: number; badge?: boolean; className?: string }) {
  const initials = contact.name.split(" ").map((p) => p[0]).slice(0, 2).join("");
  const hue = [...contact.name].reduce((a, ch) => a + ch.charCodeAt(0), 0) % 4;
  const tones = ["bg-avatar-1", "bg-avatar-2", "bg-avatar-3", "bg-avatar-4"];
  return (
    <div className={cn("relative shrink-0", className)} style={{ width: size, height: size }}>
      <div className={cn("flex h-full w-full items-center justify-center rounded-full font-semibold text-foreground", tones[hue])} style={{ fontSize: size * 0.36 }}>
        {initials}
      </div>
      {badge && contact.registered && (
        <span className={cn("absolute -bottom-0.5 -right-0.5 rounded-full border-2 border-background", contact.presence === "online" ? "bg-primary" : "bg-primary/45")}
          style={{ width: size * 0.3, height: size * 0.3 }} />
      )}
    </div>
  );
}
