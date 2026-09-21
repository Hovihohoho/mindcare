import { Bot, Menu, Pencil, Phone, Plus, Send, ShieldAlert, Sparkles, Trash2, Users } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { Button, Card } from "@/shared";
import { AiResponseError, realtimeClient } from "@/shared/realtime/realtimeClient";
import { isAxiosError } from "axios";
import { askMindCare, deleteConversation, getConversation, listConversations, renameConversation, type ConversationSummary } from "../api/chat.api";
import { ChatBubble } from "../components/ChatBubble";
import type { ChatMessage } from "../types/chat.types";

const initialMessages: ChatMessage[] = [
  { id: "welcome", role: "assistant", content: "Chào bạn! Mình là trợ lý MindCare. Bạn muốn chia sẻ điều gì hôm nay?", createdAt: "Bây giờ" },
];

export function AiChatPage() {
  const [messages, setMessages] = useState(initialMessages);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [conversationId, setConversationId] = useState<string>();

  const refreshConversations = () => void listConversations().then(setConversations).catch(() => undefined);
  useEffect(refreshConversations, []);

  const openConversation = async (id: string) => {
    const conversation = await getConversation(id);
    setConversationId(id);
    setMessages(conversation.messages.map((message) => ({
      id: message.id,
      role: message.role,
      content: message.content,
      sources: message.sources,
      safety: safetyFromLevel(message.safetyLevel),
      createdAt: new Date(message.createdAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
    })));
  };

  const startNewConversation = () => {
    setConversationId(undefined);
    setMessages(initialMessages);
  };
  const send = async (event: FormEvent) => {
    event.preventDefault();
    if (!input.trim() || loading) return;
    const question = input.trim();
    const requestId = crypto.randomUUID();
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
          response = await realtimeClient.askAi(question, history, 5, conversationId, requestId);
        } catch (error) {
          if (error instanceof AiResponseError) throw error;
          response = await askMindCare(question, history, conversationId, requestId);
        }
      } else {
        response = await askMindCare(question, history, conversationId, requestId);
      }
      setMessages((items) => [...items, { id: crypto.randomUUID(), role: "assistant", content: response.answer, sources: response.sources, safety: response.safety, createdAt: "Bây giờ" }]);
      setConversationId(response.conversationId);
      refreshConversations();
    } catch (error) {
      const content = error instanceof AiResponseError ? error.message
        : isAxiosError(error) && error.response?.status === 429
          ? "Trợ lý đang xử lý yêu cầu hoặc bạn gửi quá nhanh. Vui lòng chờ một phút rồi thử lại."
          : "Không thể kết nối với dịch vụ trợ lý. Vui lòng thử lại sau.";
      setMessages((items) => [...items, { id: crypto.randomUUID(), role: "assistant", content, createdAt: "Bây giờ" }]);
    } finally { setLoading(false); }
  };
  return (
    <Card className="grid min-h-[760px] overflow-hidden rounded-none lg:grid-cols-[280px_1fr]">
      <aside className="hidden border-r border-line bg-white p-5 lg:flex lg:flex-col">
        <div className="rounded-xl bg-slate-100 p-4"><p className="font-semibold">Trợ lý MindCare</p><p className="mt-1 text-xs text-emerald-600">● Đang trực tuyến</p></div>
        <p className="mt-8 text-xs font-extrabold tracking-wider text-muted">TRÒ CHUYỆN GẦN ĐÂY</p>
        <div className="mt-3 flex-1 space-y-1 overflow-y-auto">{conversations.length === 0 && <p className="text-sm text-muted">Chưa có cuộc trò chuyện đã lưu.</p>}{conversations.map((conversation) => <div className={`group flex items-center rounded-xl ${conversation.id === conversationId ? "bg-brand-50" : "hover:bg-slate-50"}`} key={conversation.id}><button className="min-w-0 flex-1 truncate px-3 py-2 text-left text-sm" onClick={() => void openConversation(conversation.id)}>{conversation.title}</button><button aria-label="Đổi tên" className="p-2 text-muted opacity-0 group-hover:opacity-100" onClick={() => { const title = window.prompt("Tên cuộc trò chuyện", conversation.title); if (title?.trim()) void renameConversation(conversation.id, title.trim()).then(refreshConversations); }}><Pencil className="size-3.5" /></button><button aria-label="Xóa" className="p-2 text-red-600 opacity-0 group-hover:opacity-100" onClick={() => { if (window.confirm("Xóa cuộc trò chuyện này?")) void deleteConversation(conversation.id).then(() => { if (conversation.id === conversationId) startNewConversation(); refreshConversations(); }); }}><Trash2 className="size-3.5" /></button></div>)}</div>
        <Button className="mt-4 w-full bg-emerald-700 hover:bg-emerald-800" leftIcon={<Plus className="size-4" />} onClick={startNewConversation}>Cuộc trò chuyện mới</Button>
      </aside>
      <section className="flex min-w-0 flex-col">
        <header className="flex items-center gap-3 border-b border-line px-5 py-4"><button className="lg:hidden" aria-label="Mở trình đơn"><Menu /></button><span className="grid size-11 place-items-center rounded-xl bg-brand-50 text-brand-600"><Bot /></span><div><h1 className="font-extrabold">Trợ lý MindCare</h1><p className="flex items-center gap-1.5 text-xs text-emerald-600"><span className="size-2 rounded-full bg-emerald-500" />Đang trực tuyến</p></div></header>
        <div className="flex-1 space-y-6 overflow-y-auto bg-[#f8fafc] p-5 md:p-10">{messages.map((message) => <div key={message.id}><ChatBubble message={message} />{message.safety?.showSafetyCheck && <SafetyCard safety={message.safety} />}</div>)}{loading && <p className="flex items-center gap-2 text-sm text-muted"><Sparkles className="size-4 animate-pulse text-brand-600" />Trợ lý MindCare đang tổng hợp thông tin...</p>}</div>
        <form className="border-t border-line p-4" onSubmit={send}><div className="flex gap-3 rounded-2xl border border-line bg-white p-2 focus-within:border-brand-500"><textarea className="max-h-32 min-h-11 flex-1 resize-none border-0 bg-transparent px-3 py-2 text-sm outline-none" placeholder="Nhập tin nhắn của bạn..." value={input} onChange={(event) => setInput(event.target.value)} /><Button size="icon" aria-label="Gửi"><Send className="size-5" /></Button></div><p className="mt-2 text-center text-[11px] text-muted">Hội thoại được lưu vào tài khoản và có thể xóa trong lịch sử. Nội dung không thay thế chẩn đoán hoặc dịch vụ khẩn cấp.</p></form>
      </section>
    </Card>
  );
}

function safetyFromLevel(level: NonNullable<ChatMessage["safety"]>["level"]): NonNullable<ChatMessage["safety"]> {
  return {
    level,
    showSafetyCheck: level !== "NONE",
    showEmergencyActions: level === "EXPLICIT" || level === "IMMINENT",
    emergencyNumber: level === "EXPLICIT" || level === "IMMINENT" ? "115" : null,
  };
}

function SafetyCard({ safety }: { safety: NonNullable<ChatMessage["safety"]> }) {
  const urgent = safety.showEmergencyActions;
  return (
    <div className={`mt-3 max-w-2xl rounded-2xl border p-4 ${urgent ? "border-red-300 bg-red-50" : "border-amber-300 bg-amber-50"}`} role="alert">
      <p className="flex items-center gap-2 font-bold text-slate-900"><ShieldAlert className="size-5" />{urgent ? "Ưu tiên an toàn ngay lúc này" : "MindCare muốn kiểm tra sự an toàn của bạn"}</p>
      <p className="mt-2 text-sm text-slate-700">{urgent ? "Nếu bạn có thể gây hại cho bản thân ngay lúc này, đừng ở một mình. Hãy gọi hỗ trợ khẩn cấp hoặc đến khoa cấp cứu gần nhất." : "Bạn có đang nghĩ đến việc làm hại bản thân hoặc cảm thấy mình không thể giữ an toàn không?"}</p>
      {urgent && <div className="mt-3 flex flex-wrap gap-2"><a className="inline-flex items-center gap-2 rounded-xl bg-red-700 px-4 py-2 text-sm font-bold text-white" href={`tel:${safety.emergencyNumber ?? "115"}`}><Phone className="size-4" />Gọi cấp cứu {safety.emergencyNumber ?? "115"}</a><span className="inline-flex items-center gap-2 rounded-xl border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-slate-800"><Users className="size-4" />Nhờ người tin cậy ở bên</span></div>}
      <p className="mt-3 text-xs text-slate-600">MindCare không tự động gọi cấp cứu hoặc thông báo cho người khác.</p>
    </div>
  );
}
