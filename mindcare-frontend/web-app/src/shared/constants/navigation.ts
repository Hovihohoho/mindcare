import { BookHeart, Bot, ClipboardCheck, HeartPulse, Home, Target } from "lucide-react";

export const userNavigation = [
  { label: "Trang chủ", to: "/", icon: Home },
  { label: "Đánh giá tâm lý", to: "/assessments", icon: ClipboardCheck },
  { label: "Nhật ký cảm xúc", to: "/emotion", icon: HeartPulse },
  { label: "AI hỗ trợ", to: "/ai-chat", icon: Bot },
  { label: "Tự chăm sóc", to: "/self-care", icon: BookHeart },
  { label: "Kế hoạch", to: "/care-plan", icon: Target },
] as const;
