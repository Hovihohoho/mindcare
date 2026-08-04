/* Hallmark · page: expert chat · genre: modern-minimal · theme: MindCare
 * states: loading · data · empty · error · disabled · sending
 * contrast: pass · mobile: pass
 * pre-emit critique: P5 H5 E5 S5 R5 V4
 */
import { useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CalendarClock, MessageSquareText, Send, ShieldCheck } from "lucide-react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { Avatar, Badge, Button, Card, Loading, cn } from "@/shared";
import { useCurrentUser } from "@/features/auth";
import { bookingApi } from "@/features/booking/api/booking.api";
import type { Booking } from "@/features/booking/types/booking.types";
import { useExpert } from "@/features/expert-directory";
import { expertChatApi } from "../api/expertChat.api";

function conversationTitle(booking?: Booking, expertName?: string) {
  if (!booking) return "Tư vấn với chuyên gia";
  if (expertName) return expertName;
  return `Cuộc tư vấn ${booking.id.slice(0, 8)}`;
}

function statusTone(status?: string) {
  if (status === "CONFIRMED" || status === "COMPLETED") return "success";
  if (status === "CANCELLATION_PENDING") return "warning";
  if (status === "CANCELED") return "danger";
  return "neutral";
}

const CHAT_ELIGIBLE_STATUSES = ["CONFIRMED", "CANCELLATION_PENDING", "COMPLETED"];

const BOOKING_STATUS_LABELS: Record<string, string> = {
  PAYMENT_PENDING: "Chờ thanh toán",
  CONFIRMED: "Đã xác nhận",
  CANCELLATION_PENDING: "Đang chờ duyệt hủy",
  CANCELED: "Đã hủy",
  COMPLETED: "Đã hoàn thành",
  USER_NO_SHOW: "Người dùng vắng mặt",
  EXPERT_NO_SHOW: "Chuyên gia vắng mặt",
  PAYMENT_FAILED: "Thanh toán thất bại",
  EXPIRED: "Đã hết hạn",
};

export function ExpertChatPage() {
  const { bookingId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const currentUser = useCurrentUser();
  const [content, setContent] = useState("");
  const listRef = useRef<HTMLDivElement>(null);
  const expertId = new URLSearchParams(location.search).get("expertId");
  const role = currentUser.data?.role;

  const userBookings = useQuery({
    queryKey: ["bookings", "chat-entry", expertId],
    queryFn: () => bookingApi.history({ limit: 100 }),
    enabled: !bookingId && Boolean(expertId) && role === "ROLE_USER",
  });

  const bookingsForExpert = useMemo(() => {
    const items = userBookings.data?.items ?? [];
    return items.filter((item) => item.expertUserId === expertId);
  }, [expertId, userBookings.data?.items]);

  const matchedBooking = useMemo(
    () => bookingsForExpert.find((item) => CHAT_ELIGIBLE_STATUSES.includes(item.status)),
    [bookingsForExpert],
  );
  const latestBooking = bookingsForExpert[0];

  useEffect(() => {
    if (!bookingId && matchedBooking) navigate(`/chat/${matchedBooking.id}`, { replace: true });
  }, [bookingId, matchedBooking, navigate]);

  const activeBookingId = bookingId ?? matchedBooking?.id;
  const booking = useQuery({
    queryKey: ["booking-detail", role, activeBookingId],
    queryFn: () => bookingApi.detail(activeBookingId!, role),
    enabled: Boolean(activeBookingId && role),
  });
  const expert = useExpert(booking.data?.expertUserId ?? "");
  const conversation = useQuery({
    queryKey: ["conversation", activeBookingId],
    queryFn: () => expertChatApi.getOrCreateConversation(activeBookingId!),
    enabled: Boolean(activeBookingId),
    retry: 1,
  });
  const messages = useQuery({
    queryKey: ["conversation-messages", conversation.data?.id],
    queryFn: () => expertChatApi.messages(conversation.data!.id),
    enabled: Boolean(conversation.data?.id),
    refetchInterval: 30_000,
  });

  useEffect(() => {
    if (conversation.data?.id) expertChatApi.markRead(conversation.data.id).catch(() => undefined);
  }, [conversation.data?.id, messages.data?.items.length]);

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight });
  }, [messages.data?.items.length]);

  const sendMessage = useMutation({
    mutationFn: () => expertChatApi.send(conversation.data!.id, content.trim()),
    onSuccess: async () => {
      setContent("");
      await queryClient.invalidateQueries({ queryKey: ["conversation-messages", conversation.data?.id] });
    },
  });

  if (!bookingId && expertId && (currentUser.isLoading || userBookings.isLoading)) return <Loading />;
  if (!bookingId && expertId && role !== "ROLE_USER") {
    return (
      <Card className="mx-auto max-w-xl p-8 text-center">
        <MessageSquareText className="mx-auto size-12 text-brand-700" />
        <h1 className="mt-4 text-2xl font-bold text-slate-900">Cần đăng nhập tài khoản người dùng</h1>
        <p className="mt-3 text-sm leading-6 text-slate-600">Bạn cần đăng nhập bằng tài khoản đã đặt lịch để mở phòng chat với chuyên gia.</p>
        <Link className="mt-6 inline-block" to="/login">
          <Button>Đăng nhập</Button>
        </Link>
      </Card>
    );
  }
  if (!bookingId && expertId && userBookings.isError) {
    return (
      <Card className="mx-auto max-w-xl p-8 text-center">
        <MessageSquareText className="mx-auto size-12 text-rose-500" />
        <h1 className="mt-4 text-2xl font-bold text-slate-900">Không thể tải lịch sử đặt lịch</h1>
        <p className="mt-3 text-sm leading-6 text-slate-600">
          Hệ thống chưa lấy được danh sách booking của bạn. Vui lòng kiểm tra kết nối rồi thử lại.
        </p>
        <Button className="mt-6" onClick={() => userBookings.refetch()}>
          Thử lại
        </Button>
      </Card>
    );
  }
  if (!bookingId && expertId && latestBooking && !matchedBooking) {
    return (
      <Card className="mx-auto max-w-xl p-8 text-center">
        <CalendarClock className="mx-auto size-12 text-amber-500" />
        <h1 className="mt-4 text-2xl font-bold text-slate-900">Lịch tư vấn chưa sẵn sàng để chat</h1>
        <p className="mt-3 text-sm leading-6 text-slate-600">
          Bạn đã có booking với chuyên gia này, nhưng booking hiện chưa ở trạng thái có thể mở phòng chat.
        </p>
        <div className="mx-auto mt-5 flex max-w-sm items-center justify-between rounded-xl bg-slate-50 px-4 py-3 text-sm">
          <span className="text-slate-500">Trạng thái booking</span>
          <Badge tone={statusTone(latestBooking.status)}>
            {BOOKING_STATUS_LABELS[latestBooking.status] ?? latestBooking.status}
          </Badge>
        </div>
        <p className="mt-4 text-xs leading-5 text-slate-500">
          Phòng chat chỉ được mở cho lịch đã xác nhận và trong khung thời gian tư vấn.
        </p>
        <Link className="mt-6 inline-block" to={`/experts/${expertId}`}>
          <Button variant="outline">Xem hồ sơ chuyên gia</Button>
        </Link>
      </Card>
    );
  }
  if (!bookingId && expertId && !matchedBooking) {
    return (
      <Card className="mx-auto max-w-xl p-8 text-center">
        <MessageSquareText className="mx-auto size-12 text-brand-700" />
        <h1 className="mt-4 text-2xl font-bold text-slate-900">Chưa có lịch tư vấn để chat</h1>
        <p className="mt-3 text-sm leading-6 text-slate-600">Bạn cần có một lịch tư vấn đã xác nhận với chuyên gia này trước khi mở phòng chat.</p>
        <Link className="mt-6 inline-block" to={`/booking/${expertId}`}>
          <Button>Đặt lịch tư vấn</Button>
        </Link>
      </Card>
    );
  }

  const mineId = currentUser.data?.id;
  const orderedMessages = [...(messages.data?.items ?? [])].reverse();
  const canSend = Boolean(content.trim() && conversation.data?.writable && !sendMessage.isPending);

  return (
    <Card className="mx-auto grid min-h-[720px] max-w-[1120px] overflow-hidden rounded-xl lg:grid-cols-[280px_minmax(0,1fr)]">
      <aside className="border-b border-line bg-white p-5 lg:border-b-0 lg:border-r">
        <Link className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-brand-700" to={role === "ROLE_EXPERT" ? "/expert" : "/experts"}>
          <ArrowLeft className="size-4" />Quay lại
        </Link>
        <div className="mt-6 flex items-center gap-3">
          <Avatar fallback={conversationTitle(booking.data, expert.data?.displayName)} />
          <div className="min-w-0">
            <h1 className="truncate text-base font-bold text-slate-900">{conversationTitle(booking.data, expert.data?.displayName)}</h1>
            <p className="truncate text-xs text-slate-500">Booking {activeBookingId?.slice(0, 8)}</p>
          </div>
        </div>
        <div className="mt-5 space-y-3 text-sm">
          <div className="flex items-center justify-between gap-3">
            <span className="text-slate-500">Trạng thái</span>
            <Badge tone={statusTone(booking.data?.status)}>{booking.data?.status ?? "Đang tải"}</Badge>
          </div>
          <div className="flex items-start gap-2 rounded-lg bg-slate-50 p-3 text-slate-600">
            <CalendarClock className="mt-0.5 size-4 shrink-0" />
            <span>{booking.data?.confirmedAt ? new Date(booking.data.confirmedAt).toLocaleString("vi-VN") : "Thời gian tư vấn sẽ hiển thị theo booking."}</span>
          </div>
        </div>
        <p className="mt-6 flex items-start gap-2 text-xs leading-5 text-slate-500">
          <ShieldCheck className="mt-0.5 size-4 shrink-0 text-emerald-600" />
          Chỉ người đặt lịch và chuyên gia của booking này có quyền xem hội thoại.
        </p>
      </aside>

      <section className="flex min-w-0 flex-col">
        <header className="flex items-center justify-between gap-3 border-b border-line px-5 py-4">
          <div>
            <h2 className="font-bold text-slate-900">Phòng chat tư vấn</h2>
            <p className="text-xs text-emerald-700">REST chat đang hoạt động</p>
          </div>
        </header>

        <div ref={listRef} className="flex-1 space-y-4 overflow-y-auto bg-slate-50 p-4 sm:p-6">
          {(conversation.isLoading || messages.isLoading) && <Loading />}
          {conversation.isError && (
            <p className="rounded-xl bg-amber-50 p-4 text-sm leading-6 text-amber-800">
              Chưa thể mở chat. Phòng chat chỉ mở trong khung thời gian tư vấn và với booking hợp lệ.
            </p>
          )}
          {messages.isError && (
            <div className="rounded-xl bg-rose-50 p-4 text-sm leading-6 text-rose-800">
              <p>Không thể tải lịch sử tin nhắn. Tin nhắn của bạn vẫn được lưu trên hệ thống.</p>
              <Button className="mt-3" size="sm" variant="outline" onClick={() => messages.refetch()}>
                Thử tải lại
              </Button>
            </div>
          )}
          {!conversation.isError && !messages.isError && orderedMessages.length === 0 && !messages.isLoading && (
            <div className="grid min-h-64 place-items-center text-center">
              <div>
                <MessageSquareText className="mx-auto size-10 text-slate-400" />
                <p className="mt-3 font-semibold text-slate-700">Chưa có tin nhắn nào</p>
                <p className="mt-1 text-sm text-slate-500">Bạn có thể bắt đầu trao đổi khi phòng chat đang mở.</p>
              </div>
            </div>
          )}
          {orderedMessages.map((message) => {
            const mine = message.senderId === mineId;
            return (
              <div className={cn("flex gap-3", mine && "flex-row-reverse")} key={message.id}>
                <Avatar className="size-8" fallback={mine ? currentUser.data?.fullName ?? "ME" : expert.data?.displayName ?? "CG"} />
                <div className={cn("max-w-[78%] rounded-2xl px-4 py-3 text-sm leading-6 shadow-sm", mine ? "rounded-tr-sm bg-brand-700 text-white" : "rounded-tl-sm border border-line bg-white text-slate-700")}>
                  <p className="whitespace-pre-wrap">{message.content}</p>
                  <time className={cn("mt-1 block text-[11px]", mine ? "text-white/70" : "text-slate-400")}>{new Date(message.createdAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}</time>
                </div>
              </div>
            );
          })}
        </div>

        <form className="border-t border-line bg-white p-3 sm:p-4" onSubmit={(event: FormEvent) => {
          event.preventDefault();
          if (canSend) sendMessage.mutate();
        }}>
          <div className="flex items-end gap-3 rounded-xl border border-line bg-white p-2 focus-within:border-brand-500">
            <textarea
              className="max-h-32 min-h-11 flex-1 resize-none border-0 bg-transparent px-3 py-2 text-sm outline-none placeholder:text-slate-400"
              placeholder="Nhập tin nhắn..."
              value={content}
              onChange={(event) => setContent(event.target.value)}
              disabled={!conversation.data?.writable || sendMessage.isPending}
            />
            <Button size="icon" aria-label="Gửi tin nhắn" disabled={!canSend} loading={sendMessage.isPending}>
              <Send className="size-5" />
            </Button>
          </div>
          {conversation.data && !conversation.data.writable && (
            <p className="mt-2 flex items-center justify-center gap-2 text-xs text-slate-500">
              <ShieldCheck className="size-3.5" />
              Cuộc trò chuyện đang ở chế độ chỉ đọc. Bạn vẫn có thể xem toàn bộ lịch sử phía trên.
            </p>
          )}
          {sendMessage.isError && <p className="mt-2 text-xs text-rose-700">Không thể gửi tin nhắn. Vui lòng thử lại hoặc kiểm tra khung giờ tư vấn.</p>}
        </form>
      </section>
    </Card>
  );
}
