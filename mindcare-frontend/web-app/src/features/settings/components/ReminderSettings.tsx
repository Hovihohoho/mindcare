import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BellRing } from "lucide-react";
import { Card, Loading } from "@/shared";
import { reminderApi, type ReminderPreference, type ReminderType } from "../api/reminder.api";
const labels: Record<ReminderType, { title: string; description: string }> = {
  DAILY_CHECK_IN: { title: "Check-in cảm xúc", description: "Một lời nhắc nhẹ để ghi nhận cảm xúc trong ngày." },
  SELF_CARE: { title: "Tự chăm sóc", description: "Nhắc bạn dành thời gian cho hoạt động trong kế hoạch." },
};
export function ReminderSettings() {
  const client = useQueryClient();
  const query = useQuery({ queryKey: ["reminders"], queryFn: reminderApi.list });
  const save = useMutation({ mutationFn: ({ type, value }: { type: ReminderType; value: ReminderPreference }) => reminderApi.update(type, value), onSuccess: () => client.invalidateQueries({ queryKey: ["reminders"] }) });
  if (query.isLoading) return <Card className="p-7"><Loading /></Card>;
  return <Card className="p-7"><div className="flex items-start gap-3"><BellRing className="mt-1 size-6 text-brand-600" /><div><h2 className="text-xl font-bold">Nhắc nhở hằng ngày</h2><p className="mt-1 text-sm text-muted">Bạn quyết định loại nhắc và thời điểm phù hợp. Múi giờ: Việt Nam.</p></div></div><div className="mt-6 divide-y divide-line">{query.data?.map((item) => <div className="grid gap-4 py-5 sm:grid-cols-[1fr_auto_auto] sm:items-center" key={item.reminderType}><div><p className="font-semibold">{labels[item.reminderType].title}</p><p className="mt-1 text-sm text-muted">{labels[item.reminderType].description}</p></div><input aria-label={`Giờ nhắc ${labels[item.reminderType].title}`} className="rounded-xl border border-line px-3 py-2" disabled={!item.enabled || save.isPending} type="time" value={item.localTime.slice(0, 5)} onChange={(event) => save.mutate({ type: item.reminderType, value: { ...item, localTime: event.target.value } })} /><label className="flex items-center gap-2 text-sm font-semibold"><input checked={item.enabled} disabled={save.isPending} type="checkbox" onChange={(event) => save.mutate({ type: item.reminderType, value: { ...item, enabled: event.target.checked } })} />{item.enabled ? "Đang bật" : "Đang tắt"}</label></div>)}</div>{query.isError && <p className="mt-4 text-sm text-rose-600">Không tải được lịch nhắc.</p>}{save.isError && <p className="mt-4 text-sm text-rose-600">Không thể lưu thay đổi. Vui lòng thử lại.</p>}</Card>;
}
