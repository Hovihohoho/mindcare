import { Bot, ExternalLink } from "lucide-react";
import { Avatar, cn } from "@/shared";
import type { ChatMessage } from "../types/chat.types";

export function ChatBubble({ message }: { message: ChatMessage }) {
  const assistant = message.role === "assistant";
  return (
    <div className={cn("flex gap-3", !assistant && "flex-row-reverse")}>
      {assistant ? <span className="grid size-9 shrink-0 place-items-center rounded-full bg-brand-600 text-white"><Bot className="size-5" /></span> : <Avatar className="size-9" fallback="MT" />}
      <div className={cn("max-w-[70%] rounded-2xl border px-6 py-5 text-base leading-7 shadow-sm", assistant ? "rounded-tl-sm border-line bg-white text-slate-700" : "rounded-tr-sm border-brand-700 bg-brand-700 text-white")}>
        <p className="whitespace-pre-line">{message.content}</p>
        {assistant && message.sources && message.sources.length > 0 && (
          <div className="mt-4 border-t border-slate-200 pt-3">
            <p className="text-xs font-bold uppercase tracking-wide text-slate-500">Nguồn tham khảo</p>
            <ol className="mt-2 space-y-2 text-sm">
              {message.sources.map((source, index) => (
                <li key={source.id ?? `${source.sourceUrl}-${index}`}>
                  <a className="inline-flex items-start gap-1 font-semibold text-brand-700 underline" href={source.sourceUrl} rel="noreferrer" target="_blank">
                    [{source.citationNumber}] {source.title}<ExternalLink className="mt-1 size-3.5 shrink-0" />
                  </a>
                </li>
              ))}
            </ol>
          </div>
        )}
        <span className={cn("mt-1 block text-[10px]", assistant ? "text-slate-400" : "text-white/60")}>{message.createdAt}</span>
      </div>
    </div>
  );
}
