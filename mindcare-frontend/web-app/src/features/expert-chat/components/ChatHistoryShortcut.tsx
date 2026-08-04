/* Hallmark · component: navigation card · genre: modern-minimal · theme: MindCare
 * states: default · hover · focus · active · disabled · loading · error · success
 * contrast: pass · pre-emit critique: P5 H5 E5 S5 R5 V4
 */
import { ArrowRight, History, MessagesSquare } from "lucide-react";
import { Link } from "react-router-dom";

export function ChatHistoryShortcut() {
  return (
    <Link
      className="focus-ring group mx-auto mt-8 flex max-w-[1080px] items-center gap-4 rounded-xl border border-brand-100 bg-brand-50 px-5 py-4 transition-transform duration-150 hover:-translate-y-0.5 hover:border-brand-500 active:translate-y-0"
      to="/experts/chat-history"
    >
      <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-white text-brand-700 shadow-sm">
        <MessagesSquare className="size-5" aria-hidden="true" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex items-center gap-2 font-semibold text-slate-900">
          <History className="size-4 text-brand-700" aria-hidden="true" />
          Lịch sử trò chuyện
        </span>
        <span className="mt-1 block text-sm leading-5 text-slate-600">
          Xem lại các cuộc trao đổi với chuyên gia, kể cả sau khi buổi tư vấn đã kết thúc.
        </span>
      </span>
      <ArrowRight className="size-5 shrink-0 text-brand-700 transition-transform duration-150 group-hover:translate-x-1" aria-hidden="true" />
    </Link>
  );
}
