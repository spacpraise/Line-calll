import { Link } from "@tanstack/react-router";
import { Clock, Users, Grid3x3, UserCircle } from "lucide-react";
import type { ReactNode } from "react";

const tabs = [
  { to: "/", label: "Recents", icon: Clock },
  { to: "/contacts", label: "Contacts", icon: Users },
  { to: "/dialer", label: "Keypad", icon: Grid3x3 },
  { to: "/settings", label: "Profile", icon: UserCircle },
] as const;

export function Shell({ title, action, children }: { title: string; action?: ReactNode; children: ReactNode }) {
  return (
    <div className="app-frame">
      <header className="sticky top-0 z-10 flex items-end justify-between bg-background/85 px-5 pb-3 pt-6 backdrop-blur-xl">
        <h1 className="text-[32px] font-bold tracking-tight">{title}</h1>
        {action}
      </header>
      <main className="flex-1 pb-28 animate-in fade-in slide-in-from-bottom-2 duration-300">{children}</main>
      <nav className="fixed bottom-0 left-1/2 z-20 w-full max-w-[480px] -translate-x-1/2 border-t border-border bg-background/90 pb-[env(safe-area-inset-bottom)] backdrop-blur-xl">
        <ul className="grid grid-cols-4">
          {tabs.map(({ to, label, icon: Icon }) => (
            <li key={to}>
              <Link to={to} activeOptions={{ exact: true }} className="tap flex flex-col items-center gap-1 py-2.5 text-[11px] font-medium text-muted-foreground"
                activeProps={{ className: "!text-foreground [&_.pill]:bg-accent" }}>
                <span className="pill rounded-full px-5 py-1 transition-colors"><Icon className="h-5 w-5" /></span>
                {label}
              </Link>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  );
}
