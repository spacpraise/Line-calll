import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Mic, MicOff, Volume2, Grid3x3, PhoneOff, Video, UserPlus, Lock } from "lucide-react";
import { useEffect, useState } from "react";
import { Avatar } from "@/components/app/Avatar";
import { CtrlBtn } from "@/components/app/CallControls";
import { getContact } from "@/lib/store";

export const Route = createFileRoute("/call/$id")({
  head: () => ({ meta: [
    { title: "On a call — Line Call" },
    { name: "description", content: "Crystal-clear internet calling with Line Call." },
    { property: "og:title", content: "On a call — Line Call" },
    { property: "og:description", content: "Crystal-clear internet calling with Line Call." },
  ] }),
  component: CallScreen,
});

function CallScreen() {
  const { id } = Route.useParams();
  const c = getContact(id) ?? getContact("1")!;
  const nav = useNavigate();
  const [connected, setConnected] = useState(false);
  const [secs, setSecs] = useState(0);
  const [mute, setMute] = useState(false);
  const [speaker, setSpeaker] = useState(false);
  const [ending, setEnding] = useState(false);

  useEffect(() => { const t = setTimeout(() => setConnected(true), 2800); return () => clearTimeout(t); }, []);
  useEffect(() => { if (!connected) return; const t = setInterval(() => setSecs((s) => s + 1), 1000); return () => clearInterval(t); }, [connected]);
  const end = () => { setEnding(true); setTimeout(() => nav({ to: "/" }), 600); };
  const time = `${Math.floor(secs / 60)}:${String(secs % 60).padStart(2, "0")}`;

  return (
    <div className="call-screen">
      <div className="flex flex-1 flex-col items-center pt-20 animate-in fade-in zoom-in-95 duration-500">
        <p className="mb-8 flex items-center gap-1.5 text-xs text-call-foreground/60"><Lock className="h-3 w-3" />End-to-end encrypted</p>
        <div className="relative">
          {!connected && <><span className="ring-pulse" /><span className="ring-pulse [animation-delay:0.8s]" /></>}
          <Avatar contact={c} size={128} badge={false} className="relative" />
        </div>
        <h1 className="mt-7 text-3xl font-semibold">{c.name}</h1>
        <p className="mt-2 text-call-foreground/70 tabular-nums">{ending ? "Call ended" : connected ? time : "Calling…"}</p>
      </div>
      <div className="px-8 pb-12">
        <div className="mb-10 grid grid-cols-3 gap-y-6 justify-items-center">
          <CtrlBtn icon={mute ? <MicOff /> : <Mic />} label="Mute" active={mute} onClick={() => setMute(!mute)} />
          <CtrlBtn icon={<Grid3x3 />} label="Keypad" />
          <CtrlBtn icon={<Volume2 />} label="Speaker" active={speaker} onClick={() => setSpeaker(!speaker)} />
          <CtrlBtn icon={<UserPlus />} label="Add call" />
          <CtrlBtn icon={<Video />} label="Video" />
          <span />
        </div>
        <button onClick={end} aria-label="End call" className="tap mx-auto flex h-[72px] w-[72px] items-center justify-center rounded-full bg-destructive text-destructive-foreground">
          <PhoneOff className="h-8 w-8" />
        </button>
      </div>
    </div>
  );
}
