import { cn } from "@/lib/utils";
import type { ReactNode } from "react";
export function CtrlBtn({ icon, label, active, onClick }: { icon: ReactNode; label: string; active?: boolean; onClick?: () => void }) {
  return (
    <button onClick={onClick} className="tap flex flex-col items-center gap-2" aria-pressed={active}>
      <span className={cn("flex h-16 w-16 items-center justify-center rounded-full transition-colors", active ? "bg-call-foreground text-call" : "bg-call-foreground/12 text-call-foreground")}>{icon}</span>
      <span className="text-xs text-call-foreground/75">{label}</span>
    </button>
  );
}
