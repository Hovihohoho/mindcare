import { Bot, BrainCircuit, HeartPulse, UsersRound } from "lucide-react";

export const homeServices = [
  { icon: BrainCircuit, title: "Đánh giá tâm lý", description: "Thực hiện các bài đánh giá theo tiêu chuẩn giúp nhận biết sớm các dấu hiệu về sức khỏe tinh thần.", to: "/assessments", tone: "bg-sky-200" },
  { icon: HeartPulse, title: "Nhật ký cảm xúc", description: "Ghi lại cảm xúc hằng ngày, theo dõi sự thay đổi và phân tích xu hướng cảm xúc theo thời gian.", to: "/emotion", tone: "bg-emerald-200" },
  { icon: Bot, title: "AI hỗ trợ", description: "Chatbot AI hoạt động 24/7, cung cấp hướng dẫn sơ cứu tâm lý và hỗ trợ ban đầu mọi lúc.", to: "/ai-chat", tone: "bg-sky-300" },
  { icon: UsersRound, title: "Chuyên gia tư vấn", description: "Đặt lịch tư vấn trực tuyến với chuyên gia hoặc mentor uy tín, đảm bảo riêng tư tuyệt đối.", to: "/experts", tone: "bg-orange-200" },
] as const;
