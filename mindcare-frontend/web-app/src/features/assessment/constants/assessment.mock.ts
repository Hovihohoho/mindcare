import type { Assessment } from "../types/assessment.types";

const options = ["Không bao giờ", "Hiếm khi", "Đôi khi", "Thường xuyên", "Rất thường xuyên"].map((label, value) => ({ id: String(value), label, value }));

export const assessmentMocks: Assessment[] = [
  { code: "GAD_7", title: "Lo âu tổng quát (GAD-7)", description: "Đánh giá mức độ lo âu phổ quát trong 2 tuần gần nhất qua các biểu hiện tâm lý và thể chất.", durationMinutes: 5, questionCount: 7, category: "Lo âu" },
  { code: "PHQ_9", title: "Trầm cảm (PHQ-9)", description: "Công cụ tầm soát mức độ trầm cảm dựa trên các triệu chứng lâm sàng phổ biến nhất.", durationMinutes: 8, questionCount: 9, category: "Trầm cảm" },
  { code: "PSS_10", title: "Stress (PSS-10)", description: "Đo lường mức độ căng thẳng mà bạn cảm nhận được trong các tình huống cuộc sống hàng ngày.", durationMinutes: 8, questionCount: 10, category: "Căng thẳng" },
  { code: "BURNOUT", title: "Kiệt sức nghề nghiệp", description: "Nhận diện sớm các dấu hiệu kiệt sức trong công việc và học tập để cân bằng kịp thời.", durationMinutes: 10, questionCount: 12, category: "Công việc" },
  { code: "RSES", title: "Lòng tự trọng (RSES)", description: "Thang đo đánh giá mức độ bạn trân trọng và tin tưởng vào giá trị bản thân.", durationMinutes: 6, questionCount: 10, category: "Bản thân" },
];

export const gadQuestions = [
  "Trong 2 tuần qua, bạn có thường xuyên cảm thấy lo lắng, bồn chồn hoặc căng thẳng không?",
  "Bạn có cảm thấy khó kiểm soát sự lo lắng của mình không?",
  "Bạn có lo lắng quá nhiều về những điều khác nhau không?",
  "Bạn có cảm thấy khó thư giãn không?",
  "Bạn có bồn chồn đến mức khó ngồi yên không?",
  "Bạn có dễ trở nên khó chịu hoặc cáu kỉnh không?",
  "Bạn có cảm thấy sợ hãi như thể điều tồi tệ có thể xảy ra không?",
].map((prompt, index) => ({ id: String(index + 1), prompt, options }));
