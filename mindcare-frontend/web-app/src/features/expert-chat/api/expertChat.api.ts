import { httpClient, type CursorPage } from "@/shared";
import type { Conversation, ConversationHistoryItem, ExpertChatMessage } from "../types/expert-chat.types";

export const expertChatApi = {
  async conversationHistory(cursor?: string): Promise<CursorPage<ConversationHistoryItem>> {
    const { data } = await httpClient.get<CursorPage<ConversationHistoryItem>>(
      "/api/v1/conversations",
      { params: { cursor, limit: 20 } },
    );
    return data;
  },
  async getOrCreateConversation(expertUserId: string): Promise<Conversation> {
    const { data } = await httpClient.post<Conversation>("/api/v1/conversations", { expertUserId });
    return data;
  },
  async conversation(conversationId: string): Promise<Conversation> {
    const { data } = await httpClient.get<Conversation>(`/api/v1/conversations/${conversationId}`);
    return data;
  },
  async messages(conversationId: string): Promise<CursorPage<ExpertChatMessage>> {
    const { data } = await httpClient.get<CursorPage<ExpertChatMessage>>(
      `/api/v1/conversations/${conversationId}/messages`,
      { params: { limit: 100 } },
    );
    return data;
  },
  async send(conversationId: string, content: string): Promise<ExpertChatMessage> {
    const { data } = await httpClient.post<ExpertChatMessage>(
      `/api/v1/conversations/${conversationId}/messages`,
      { content },
    );
    return data;
  },
  async markRead(conversationId: string): Promise<void> {
    await httpClient.post(`/api/v1/conversations/${conversationId}:read`);
  },
};
