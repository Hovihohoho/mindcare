import { httpClient, type ApiResponse } from "@/shared";
import type { ChatSource } from "../types/chat.types";

interface RagResponse {
  answer: string;
  sources: ChatSource[];
}

export async function askMindCare(question: string): Promise<RagResponse> {
  const { data } = await httpClient.post<ApiResponse<RagResponse>>("/api/ai/chat", { question, topK: 5 });
  return data.data;
}
