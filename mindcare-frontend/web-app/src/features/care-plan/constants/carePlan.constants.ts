import type { CareGoal } from "../api/carePlan.api";

export const goalLabels: Record<CareGoal, string> = {
  REDUCE_STRESS: "Giảm căng thẳng",
  IMPROVE_SLEEP: "Ngủ tốt hơn",
  MANAGE_ANXIETY: "Quản lý lo âu",
  BUILD_BALANCE: "Xây dựng cân bằng",
};
