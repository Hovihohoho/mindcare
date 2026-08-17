import { httpClient, type ApiResponse } from "@/shared";
import type { ChatSource } from "../types/chat.types";

interface RagResponse {
  answer: string;
  sources: ChatSource[];
}

export async function askMindCare(question: string, history: Array<{ role: "user" | "assistant"; content: string }> = []): Promise<RagResponse> {
  const { data } = await httpClient.post<ApiResponse<RagResponse>>("/api/ai/chat", { question, topK: 5, history });
  return data.data;
}
