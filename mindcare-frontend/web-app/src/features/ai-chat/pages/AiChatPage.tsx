import { Bot, Menu, Plus, Send, Sparkles } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Button, Card } from "@/shared";
import { realtimeClient } from "@/shared/realtime/realtimeClient";
import { askMindCare } from "../api/chat.api";
import { ChatBubble } from "../components/ChatBubble";
import type { ChatMessage } from "../types/chat.types";

const initialMessages: ChatMessage[] = [
  { id: "welcome", role: "assistant", content: "Chào bạn! Mình là trợ lý MindCare. Bạn muốn chia sẻ điều gì hôm nay?", createdAt: "Bây giờ" },
];

export function AiChatPage() {
  const [messages, setMessages] = useState(initialMessages);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const send = async (event: FormEvent) => {
    event.preventDefault();
    if (!input.trim() || loading) return;
    const question = input.trim();
    const history = messages
      .filter((message) => message.id !== "welcome")
      .slice(-6)
      .map(({ role, content }) => ({ role, content }));
    setMessages((items) => [...items, { id: crypto.randomUUID(), role: "user", content: question, createdAt: "Bây giờ" }]);
    setInput(""); setLoading(true);
    try {
      let response;
      if (realtimeClient.isOpen("ai")) {
        try {
          response = await realtimeClient.askAi(question, history);
        } catch {
          response = await askMindCare(question, history);
        }
      } else {
        response = await askMindCare(question, history);
      }
      setMessages((items) => [...items, { id: crypto.randomUUID(), role: "assistant", content: response.answer, sources: response.sources, createdAt: "Bây giờ" }]);
    } catch {
      setMessages((items) => [...items, { id: crypto.randomUUID(), role: "assistant", content: "Không thể kết nối với dịch vụ trợ lý. Vui lòng thử lại sau.", createdAt: "Bây giờ" }]);
    } finally { setLoading(false); }
  };
  return (
    <Card className="grid min-h-[760px] overflow-hidden rounded-none lg:grid-cols-[280px_1fr]">
      <aside className="hidden border-r border-line bg-white p-5 lg:flex lg:flex-col">
        <div className="rounded-xl bg-slate-100 p-4"><p className="font-semibold">Trợ lý MindCare</p><p className="mt-1 text-xs text-emerald-600">● Đang trực tuyến</p></div>
        <p className="mt-8 text-xs font-extrabold tracking-wider text-muted">TRÒ CHUYỆN GẦN ĐÂY</p><p className="mt-4 text-sm text-muted">Lịch sử trò chuyện chưa được lưu.</p>
        <Button className="mt-auto w-full bg-emerald-700 hover:bg-emerald-800" leftIcon={<Plus className="size-4" />} onClick={() => setMessages(initialMessages)}>Cuộc trò chuyện mới</Button>
      </aside>
      <section className="flex min-w-0 flex-col">
        <header className="flex items-center gap-3 border-b border-line px-5 py-4"><button className="lg:hidden" aria-label="Mở trình đơn"><Menu /></button><span className="grid size-11 place-items-center rounded-xl bg-brand-50 text-brand-600"><Bot /></span><div><h1 className="font-extrabold">Trợ lý MindCare</h1><p className="flex items-center gap-1.5 text-xs text-emerald-600"><span className="size-2 rounded-full bg-emerald-500" />Đang trực tuyến</p></div></header>
        <div className="flex-1 space-y-6 overflow-y-auto bg-[#f8fafc] p-5 md:p-10">{messages.map((message) => <ChatBubble key={message.id} message={message} />)}{loading && <p className="flex items-center gap-2 text-sm text-muted"><Sparkles className="size-4 animate-pulse text-brand-600" />Trợ lý MindCare đang tổng hợp thông tin...</p>}</div>
        <form className="border-t border-line p-4" onSubmit={send}><div className="flex gap-3 rounded-2xl border border-line bg-white p-2 focus-within:border-brand-500"><textarea className="max-h-32 min-h-11 flex-1 resize-none border-0 bg-transparent px-3 py-2 text-sm outline-none" placeholder="Nhập tin nhắn của bạn..." value={input} onChange={(event) => setInput(event.target.value)} /><Button size="icon" aria-label="Gửi"><Send className="size-5" /></Button></div><p className="mt-2 text-center text-[11px] text-muted">Nội dung chỉ mang tính tham khảo, luôn kèm nguồn và không thay thế chẩn đoán của chuyên gia y tế.</p></form>
      </section>
    </Card>
  );
}
