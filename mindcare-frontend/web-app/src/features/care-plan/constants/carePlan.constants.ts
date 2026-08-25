import type { CareActivityInput, CareGoal } from "../api/carePlan.api";

export const goalLabels: Record<CareGoal, string> = {
  REDUCE_STRESS: "Giảm căng thẳng",
  IMPROVE_SLEEP: "Ngủ tốt hơn",
  MANAGE_ANXIETY: "Quản lý lo âu",
  BUILD_BALANCE: "Xây dựng cân bằng",
};

export const activityTemplates: Record<CareGoal, CareActivityInput[]> = {
  REDUCE_STRESS: [
    { activityCode: "BREATHING_5_MIN", title: "Thở chậm trong 5 phút", targetPerWeek: 5 },
    { activityCode: "SHORT_WALK", title: "Đi bộ nhẹ nhàng", targetPerWeek: 4 },
    { activityCode: "SCREEN_BREAK", title: "Nghỉ khỏi màn hình 15 phút", targetPerWeek: 5 },
  ],
  IMPROVE_SLEEP: [
    { activityCode: "FIXED_BEDTIME", title: "Đi ngủ đúng giờ đã chọn", targetPerWeek: 5 },
    { activityCode: "NO_SCREEN_BEFORE_BED", title: "Không dùng màn hình 30 phút trước ngủ", targetPerWeek: 5 },
    { activityCode: "SLEEP_REFLECTION", title: "Ghi lại cảm nhận về giấc ngủ", targetPerWeek: 3 },
  ],
  MANAGE_ANXIETY: [
    { activityCode: "GROUNDING_54321", title: "Thực hành grounding 5-4-3-2-1", targetPerWeek: 4 },
    { activityCode: "WORRY_NOTE", title: "Viết xuống điều đang lo lắng", targetPerWeek: 3 },
    { activityCode: "BREATHING_5_MIN", title: "Thở chậm trong 5 phút", targetPerWeek: 5 },
  ],
  BUILD_BALANCE: [
    { activityCode: "DAILY_CHECK_IN", title: "Check-in cảm xúc", targetPerWeek: 5 },
    { activityCode: "GRATITUDE_NOTE", title: "Ghi lại một điều tích cực", targetPerWeek: 4 },
    { activityCode: "SHORT_WALK", title: "Đi bộ nhẹ nhàng", targetPerWeek: 4 },
  ],
};
