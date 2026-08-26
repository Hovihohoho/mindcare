import { Activity, BarChart3, BookHeart, Bot, ClipboardCheck, HeartPulse, Home, Target } from "lucide-react";

export const userNavigation = [
  { label: "Tiến triển", to: "/progress", icon: BarChart3 },
  { label: "Trang chủ", to: "/", icon: Home },
  { label: "Đánh giá tâm lý", to: "/assessments", icon: ClipboardCheck },
  { label: "Nhật ký cảm xúc", to: "/emotion", icon: HeartPulse },
  { label: "Sức khỏe", to: "/health", icon: Activity },
  { label: "AI hỗ trợ", to: "/ai-chat", icon: Bot },
  { label: "Tự chăm sóc", to: "/self-care", icon: BookHeart },
  { label: "Kế hoạch", to: "/care-plan", icon: Target },
] as const;
