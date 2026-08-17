import { Bot, ClipboardCheck, HeartPulse, Home } from "lucide-react";

export const userNavigation = [
  { label: "Trang chủ", to: "/", icon: Home },
  { label: "Đánh giá tâm lý", to: "/assessments", icon: ClipboardCheck },
  { label: "Nhật ký cảm xúc", to: "/emotion", icon: HeartPulse },
  { label: "AI hỗ trợ", to: "/ai-chat", icon: Bot },
] as const;
