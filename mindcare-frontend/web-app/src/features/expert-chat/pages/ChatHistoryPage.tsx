/* Hallmark · page: conversation history · genre: modern-minimal · theme: MindCare
 * states: loading · data · empty · error · pagination · read-only
 * contrast: pass · mobile: pass · pre-emit critique: P5 H5 E5 S5 R5 V4
 */
import { useInfiniteQuery } from "@tanstack/react-query";
import { ArrowLeft, CalendarClock, ChevronRight, History, LockKeyhole } from "lucide-react";
import { Link } from "react-router-dom";
import { Badge, Button, Card, EmptyState, Loading } from "@/shared";
import { useExpert } from "@/features/expert-directory";
import { expertChatApi } from "../api/expertChat.api";
import type { ConversationHistoryItem } from "../types/expert-chat.types";

const WRITABLE_STATUSES = ["CONFIRMED", "CANCELLATION_PENDING", "COMPLETED"];

function consultationState(item: ConversationHistoryItem) {
  const now = Date.now();
  const opensAt = new Date(item.startAt).getTime() - 24 * 60 * 60 * 1000;
  const closesAt = new Date(item.endAt).getTime() + 24 * 60 * 60 * 1000;
  if (!WRITABLE_STATUSES.includes(item.bookingStatus) || now > closesAt) {
    return { label: "Chỉ đọc", tone: "neutral" as const, readOnly: true };
  }
  if (now < opensAt) {
    return { label: "Chưa mở", tone: "warning" as const, readOnly: true };
  }
  return { label: "Đang mở", tone: "success" as const, readOnly: false };
}

function ConversationRow({ item }: { item: ConversationHistoryItem }) {
  const expert = useExpert(item.expertUserId);
  const state = consultationState(item);
  return (
    <Link
      className="focus-ring group grid gap-4 border-b border-line px-5 py-5 transition-colors hover:bg-slate-50 active:bg-slate-100 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center"
      to={`/chat/${item.bookingId}`}
    >
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="truncate font-semibold text-slate-900">
            {expert.data?.displayName ?? "Cuộc trò chuyện với chuyên gia"}
          </h2>
          <Badge tone={state.tone}>{state.label}</Badge>
        </div>
        <p className="mt-2 flex items-center gap-2 text-sm text-slate-500">
          <CalendarClock className="size-4 shrink-0" aria-hidden="true" />
          {new Date(item.startAt).toLocaleString("vi-VN", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
          })}
        </p>
        {state.readOnly && (
          <p className="mt-2 flex items-center gap-2 text-xs text-slate-500">
            <LockKeyhole className="size-3.5 shrink-0" aria-hidden="true" />
            Lịch sử vẫn được lưu và có thể xem lại; chức năng gửi tin đã đóng.
          </p>
        )}
      </div>
      <span className="flex items-center gap-2 text-sm font-semibold text-brand-700">
        Xem cuộc trò chuyện
        <ChevronRight className="size-4 transition-transform duration-150 group-hover:translate-x-1" aria-hidden="true" />
      </span>
    </Link>
  );
}

export function ChatHistoryPage() {
  const history = useInfiniteQuery({
    queryKey: ["conversation-history"],
    queryFn: ({ pageParam }) => expertChatApi.conversationHistory(pageParam),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.hasMore ? lastPage.nextCursor : undefined,
  });
  const conversations = history.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <div className="mx-auto w-full max-w-[1080px] py-6 md:py-10">
      <Link className="focus-ring inline-flex items-center gap-2 rounded-lg text-sm font-semibold text-slate-500 hover:text-brand-700" to="/experts">
        <ArrowLeft className="size-4" aria-hidden="true" />
        Quay lại danh sách chuyên gia
      </Link>

      <header className="mt-7 border-b border-line pb-6">
        <div className="flex items-start gap-4">
          <span className="grid size-12 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700">
            <History className="size-6" aria-hidden="true" />
          </span>
          <div className="min-w-0">
            <h1 className="min-w-0 text-2xl font-bold text-slate-900 [overflow-wrap:anywhere] md:text-3xl">Lịch sử trò chuyện</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">
              Tất cả cuộc trao đổi của bạn với chuyên gia được lưu tại đây. Sau thời hạn tư vấn, bạn vẫn có thể mở lại nội dung ở chế độ chỉ đọc.
            </p>
          </div>
        </div>
      </header>

      <Card className="mt-7 overflow-hidden p-0">
        {history.isLoading && <div className="p-10"><Loading /></div>}
        {history.isError && (
          <div className="p-8 text-center">
            <p className="font-semibold text-slate-800">Không thể tải lịch sử trò chuyện</p>
            <p className="mt-2 text-sm text-slate-500">Vui lòng kiểm tra kết nối và thử lại.</p>
            <Button className="mt-5" variant="outline" onClick={() => history.refetch()}>Thử lại</Button>
          </div>
        )}
        {!history.isLoading && !history.isError && conversations.length === 0 && (
          <div className="p-8">
            <EmptyState
              title="Chưa có lịch sử trò chuyện"
              description="Sau khi bạn bắt đầu trao đổi trong một lịch tư vấn, cuộc trò chuyện sẽ xuất hiện tại đây."
            />
          </div>
        )}
        {conversations.map((item) => <ConversationRow item={item} key={item.id} />)}
      </Card>

      {history.hasNextPage && (
        <div className="mt-6 text-center">
          <Button
            variant="outline"
            loading={history.isFetchingNextPage}
            onClick={() => history.fetchNextPage()}
          >
            Xem thêm cuộc trò chuyện
          </Button>
        </div>
      )}
    </div>
  );
}
