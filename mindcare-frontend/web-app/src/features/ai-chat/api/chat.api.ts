import { httpClient, type ApiResponse } from "@/shared";
import type { ChatSafetyDirective, ChatSource } from "../types/chat.types";

interface RagResponse {
  answer: string;
  sources: ChatSource[];
  safety: ChatSafetyDirective;
  conversationId: string;
}

export interface ConversationSummary {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface ConversationDetail extends ConversationSummary {
  messages: Array<{
    id: string;
    role: "user" | "assistant";
    content: string;
    sources: ChatSource[];
    safetyLevel: ChatSafetyDirective["level"];
    createdAt: string;
  }>;
}

export async function askMindCare(question: string, history: Array<{ role: "user" | "assistant"; content: string }> = [], conversationId?: string, requestId = crypto.randomUUID()): Promise<RagResponse> {
  const { data } = await httpClient.post<ApiResponse<RagResponse>>("/api/ai/chat", { question, topK: 5, history, conversationId, requestId }, { timeout: 120_000 });
  return data.data;
}

export async function listConversations() {
  const { data } = await httpClient.get<ApiResponse<ConversationSummary[]>>("/api/ai/chat/conversations");
  return data.data;
}

export async function getConversation(id: string) {
  const { data } = await httpClient.get<ApiResponse<ConversationDetail>>(`/api/ai/chat/conversations/${id}`);
  return data.data;
}

export async function renameConversation(id: string, title: string) {
  const { data } = await httpClient.patch<ApiResponse<ConversationSummary>>(`/api/ai/chat/conversations/${id}`, { title });
  return data.data;
}

export async function deleteConversation(id: string) {
  await httpClient.delete(`/api/ai/chat/conversations/${id}`);
}
