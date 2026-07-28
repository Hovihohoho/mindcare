import { httpClient, type ApiResponse } from "@/shared";
import type { ChatSource } from "../types/chat.types";

interface RagResponse {
  answer: string;
  sources: ChatSource[];
}

export async function askMindCare(question: string): Promise<RagResponse> {
  try {
    const { data } = await httpClient.post<ApiResponse<RagResponse>>("/api/ai/chat", { question, topK: 5 });
    return data.data;
  } catch {
    return {
      answer: "Mình đang ở đây và lắng nghe bạn. Khi cảm thấy áp lực, bạn có thể thử dừng lại một chút, hít vào chậm trong 4 nhịp và thở ra trong 6 nhịp. Bạn muốn kể thêm điều gì đang khiến mình mệt mỏi nhất không?",
      sources: [],
    };
  }
}
