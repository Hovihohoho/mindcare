import {
  Bot,
  CalendarDays,
  ChartNoAxesColumn,
  ClipboardCheck,
  HeartPulse,
  Home,
  LayoutDashboard,
  Settings,
  UserRound,
  UsersRound,
} from "lucide-react";

export const userNavigation = [
  { label: "Trang chủ", to: "/", icon: Home },
  { label: "Đánh giá tâm lý", to: "/assessments", icon: ClipboardCheck },
  { label: "Nhật ký cảm xúc", to: "/emotion", icon: HeartPulse },
  { label: "Chuyên gia", to: "/experts", icon: UsersRound },
  { label: "AI hỗ trợ", to: "/ai-chat", icon: Bot },
] as const;

export const expertNavigation = [
  { label: "Dashboard", to: "/expert", icon: LayoutDashboard },
  { label: "Lịch làm việc", to: "/expert/calendar", icon: CalendarDays },
  { label: "Hồ sơ chuyên gia", to: "/expert/profile", icon: UserRound },
  { label: "Thống kê", to: "/expert/statistics", icon: ChartNoAxesColumn },
] as const;

export const expertUtilityNavigation = [
  { label: "Cài đặt", to: "/expert/settings", icon: Settings },
] as const;
