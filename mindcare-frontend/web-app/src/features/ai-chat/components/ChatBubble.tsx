import { Bot } from "lucide-react";
import { Avatar, cn } from "@/shared";
import type { ChatMessage } from "../types/chat.types";

export function ChatBubble({ message }: { message: ChatMessage }) {
  const assistant = message.role === "assistant";
  return (
    <div className={cn("flex gap-3", !assistant && "flex-row-reverse")}>
      {assistant ? <span className="grid size-9 shrink-0 place-items-center rounded-full bg-brand-600 text-white"><Bot className="size-5" /></span> : <Avatar className="size-9" fallback="MT" />}
      <div className={cn("max-w-[70%] whitespace-pre-line rounded-2xl border px-6 py-5 text-base leading-7 shadow-sm", assistant ? "rounded-tl-sm border-line bg-white text-slate-700" : "rounded-tr-sm border-brand-700 bg-brand-700 text-white")}>
        {message.content}
        <span className={cn("mt-1 block text-[10px]", assistant ? "text-slate-400" : "text-white/60")}>{message.createdAt}</span>
      </div>
    </div>
  );
}
