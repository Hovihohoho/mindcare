import { Bot, Menu, Plus, Send, Sparkles } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Button, Card } from "@/shared";
import { askMindCare } from "../api/chat.api";
import { ChatBubble } from "../components/ChatBubble";
import type { ChatMessage } from "../types/chat.types";

const initialMessages: ChatMessage[] = [
  { id: "welcome", role: "assistant", content: "Chào bạn! Mình là MindCare AI. Mình ở đây để lắng nghe và hỗ trợ bạn trong những lúc cảm thấy khó khăn hoặc chỉ đơn giản là muốn trò chuyện.\n\nHôm nay của bạn thế nào? Có điều gì đang làm bạn lo lắng không?", createdAt: "14:20" },
  { id: "question", role: "user", content: "Mình cảm thấy khá mệt mỏi và áp lực với kỳ thi sắp tới. Mình không thể tập trung được.", createdAt: "14:21" },
  { id: "support", role: "assistant", content: "Mình rất hiểu cảm giác đó. Áp lực thi cử có thể khiến chúng ta cảm thấy quá tải. Đó là phản ứng hoàn toàn tự nhiên của cơ thể.\n\nĐể bắt đầu, bạn có muốn thử một bài tập thở ngắn 2 phút để lấy lại bình tĩnh, hay bạn muốn mình chia sẻ một vài mẹo nhỏ để phân chia việc học cho bớt áp lực hơn?", createdAt: "14:22" },
];

export function AiChatPage() {
  const [messages, setMessages] = useState(initialMessages);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const send = async (event: FormEvent) => {
    event.preventDefault();
    if (!input.trim() || loading) return;
    const question = input.trim();
    setMessages((items) => [...items, { id: crypto.randomUUID(), role: "user", content: question, createdAt: "Bây giờ" }]);
    setInput(""); setLoading(true);
    const response = await askMindCare(question);
    setMessages((items) => [...items, { id: crypto.randomUUID(), role: "assistant", content: response.answer, sources: response.sources, createdAt: "Bây giờ" }]);
    setLoading(false);
  };
  return (
    <Card className="grid min-h-[760px] overflow-hidden rounded-none lg:grid-cols-[280px_1fr]">
      <aside className="hidden border-r border-line bg-white p-5 lg:flex lg:flex-col"><div className="rounded-xl bg-slate-100 p-4"><p className="font-semibold">AI Trợ lý MindCare</p><p className="mt-1 text-xs text-emerald-600">● Đang trực tuyến</p></div><p className="mt-8 text-xs font-extrabold tracking-wider text-muted">TRÒ CHUYỆN GẦN ĐÂY</p><div className="mt-3 space-y-1">{["Làm sao để giảm lo âu...", "Cải thiện giấc ngủ", "Kết quả bài đánh giá..."].map((title, index) => <button className={`w-full rounded-lg px-3 py-3 text-left text-sm ${index === 0 ? "border border-sky-200 bg-sky-50 font-semibold text-brand-700" : "text-slate-600 hover:bg-slate-50"}`} key={title}>{title}</button>)}</div><Button className="mt-auto w-full bg-emerald-700 hover:bg-emerald-800" leftIcon={<Plus className="size-4" />}>Cuộc hội thoại mới</Button></aside>
      <section className="flex min-w-0 flex-col">
        <header className="flex items-center gap-3 border-b border-line px-5 py-4"><button className="lg:hidden"><Menu /></button><span className="grid size-11 place-items-center rounded-xl bg-brand-50 text-brand-600"><Bot /></span><div><h1 className="font-extrabold">AI Trợ lý MindCare</h1><p className="flex items-center gap-1.5 text-xs text-emerald-600"><span className="size-2 rounded-full bg-emerald-500" />Đang trực tuyến</p></div></header>
        <div className="flex-1 space-y-6 overflow-y-auto bg-[#f8fafc] p-5 md:p-10">{messages.map((message) => <ChatBubble key={message.id} message={message} />)}{loading && <p className="flex items-center gap-2 text-sm text-muted"><Sparkles className="size-4 animate-pulse text-brand-600" />MindCare AI đang suy nghĩ...</p>}</div>
        <form className="border-t border-line p-4" onSubmit={send}><div className="flex gap-3 rounded-2xl border border-line bg-white p-2 focus-within:border-brand-500"><textarea className="max-h-32 min-h-11 flex-1 resize-none border-0 bg-transparent px-3 py-2 text-sm outline-none" placeholder="Nhập tin nhắn của bạn..." value={input} onChange={(event) => setInput(event.target.value)} /><Button size="icon" aria-label="Gửi"><Send className="size-5" /></Button></div><p className="mt-2 text-center text-[11px] text-muted">AI chỉ cung cấp hỗ trợ tham khảo và không thay thế chuyên gia y tế.</p></form>
      </section>
    </Card>
  );
}
