/* Hallmark · page: direct expert chat · genre: modern-minimal · theme: MindCare
 * states: loading · empty · error · writable · read-only
 * pre-emit critique: P5 H5 E4 S5 R5 V4
 */
import { useEffect, useRef, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Send, ShieldCheck } from "lucide-react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { useCurrentUser } from "@/features/auth";
import { useExpert } from "@/features/expert-directory";
import { Avatar, Button, Card, Loading } from "@/shared";
import { cn } from "@/shared/lib/cn";
import { expertChatApi } from "../api/expertChat.api";

export function ExpertChatPage() {
  const { conversationId } = useParams();
  const [params] = useSearchParams();
  const expertId = params.get("expertId") ?? "";
  const [content, setContent] = useState("");
  const listRef = useRef<HTMLDivElement>(null);
  const queryClient = useQueryClient();
  const currentUser = useCurrentUser();

  const conversation = useQuery({
    queryKey: ["expert-conversation", conversationId ?? expertId],
    queryFn: () => conversationId
      ? expertChatApi.conversation(conversationId)
      : expertChatApi.getOrCreateConversation(expertId),
    enabled: Boolean(conversationId || expertId),
  });
  const resolvedConversationId = conversation.data?.id ?? conversationId;
  const resolvedExpertId = conversation.data?.expertUserId ?? expertId;
  const expert = useExpert(resolvedExpertId);
  const messages = useQuery({
    queryKey: ["expert-chat-messages", resolvedConversationId],
    queryFn: () => expertChatApi.messages(resolvedConversationId!),
    enabled: Boolean(resolvedConversationId),
  });
  const sendMessage = useMutation({
    mutationFn: () => expertChatApi.send(resolvedConversationId!, content.trim()),
    onSuccess: () => {
      setContent("");
      queryClient.invalidateQueries({ queryKey: ["expert-chat-messages", resolvedConversationId] });
    },
  });

  useEffect(() => {
    if (resolvedConversationId) expertChatApi.markRead(resolvedConversationId).catch(() => undefined);
  }, [resolvedConversationId, messages.data]);
  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight });
  }, [messages.data]);

  if (!conversationId && !expertId) {
    return <Card className="mx-auto max-w-xl p-8 text-center">Không xác định được chuyên gia hoặc cuộc trò chuyện.</Card>;
  }
  if (conversation.isLoading) return <Loading />;
  if (conversation.isError || !conversation.data) {
    return <Card className="mx-auto max-w-xl p-8 text-center text-rose-700">Không thể mở cuộc trò chuyện với chuyên gia này.</Card>;
  }

  const orderedMessages = [...(messages.data?.items ?? [])].reverse();
  const canSend = Boolean(content.trim() && conversation.data.writable && !sendMessage.isPending);

  return (
    <Card className="mx-auto grid min-h-[720px] max-w-[1120px] overflow-hidden rounded-xl lg:grid-cols-[280px_minmax(0,1fr)]">
      <aside className="border-b border-line bg-white p-5 lg:border-b-0 lg:border-r">
        <Link className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-brand-700" to="/experts">
          <ArrowLeft className="size-4" />Danh sách chuyên gia
        </Link>
        <div className="mt-7 flex items-center gap-3">
          <Avatar fallback={expert.data?.displayName ?? "CG"} />
          <div className="min-w-0">
            <h1 className="truncate font-bold text-slate-900">{expert.data?.displayName ?? "Chuyên gia MindCare"}</h1>
            <p className="text-xs text-emerald-700">Cuộc trò chuyện trực tiếp</p>
          </div>
        </div>
        <p className="mt-6 flex items-start gap-2 text-xs leading-5 text-slate-500">
          <ShieldCheck className="mt-0.5 size-4 shrink-0 text-emerald-600" />
          Chỉ bạn và chuyên gia này được quyền xem nội dung hội thoại. Chat không thay thế dịch vụ cấp cứu.
        </p>
      </aside>

      <section className="flex min-w-0 flex-col">
        <header className="border-b border-line px-5 py-4">
          <h2 className="font-bold text-slate-900">Trao đổi với chuyên gia</h2>
          <p className="text-xs text-slate-500">Không yêu cầu đặt lịch hoặc thanh toán</p>
        </header>
        <div ref={listRef} className="flex-1 space-y-4 overflow-y-auto bg-slate-50 p-4 sm:p-6">
          {messages.isLoading && <Loading />}
          {messages.isError && <p className="rounded-xl bg-rose-50 p-4 text-sm text-rose-800">Không thể tải lịch sử tin nhắn.</p>}
          {!messages.isLoading && orderedMessages.length === 0 && (
            <p className="grid min-h-64 place-items-center text-center text-sm text-slate-500">Hãy bắt đầu bằng điều bạn đang muốn được hỗ trợ.</p>
          )}
          {orderedMessages.map((message) => {
            const mine = message.senderId === currentUser.data?.id;
            return <div className={cn("flex gap-3", mine && "flex-row-reverse")} key={message.id}>
              <Avatar className="size-8" fallback={mine ? "ME" : "CG"} />
              <div className={cn("max-w-[78%] rounded-2xl px-4 py-3 text-sm leading-6 shadow-sm", mine ? "rounded-tr-sm bg-brand-700 text-white" : "rounded-tl-sm border border-line bg-white text-slate-700")}>
                <p className="whitespace-pre-wrap">{message.content}</p>
                <time className={cn("mt-1 block text-[11px]", mine ? "text-white/70" : "text-slate-400")}>{new Date(message.createdAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}</time>
              </div>
            </div>;
          })}
        </div>
        <form className="border-t border-line bg-white p-4" onSubmit={(event: FormEvent) => { event.preventDefault(); if (canSend) sendMessage.mutate(); }}>
          <div className="flex items-end gap-3 rounded-xl border border-line p-2 focus-within:border-brand-500">
            <textarea className="max-h-32 min-h-11 flex-1 resize-none border-0 bg-transparent px-3 py-2 text-sm outline-none" placeholder="Nhập tin nhắn..." value={content} onChange={(event) => setContent(event.target.value)} disabled={!conversation.data.writable} />
            <Button size="icon" aria-label="Gửi tin nhắn" disabled={!canSend} loading={sendMessage.isPending}><Send className="size-5" /></Button>
          </div>
          {sendMessage.isError && <p className="mt-2 text-xs text-rose-700">Không thể gửi tin nhắn. Vui lòng thử lại.</p>}
        </form>
      </section>
    </Card>
  );
}
