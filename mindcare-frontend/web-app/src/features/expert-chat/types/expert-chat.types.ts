export interface Conversation {
  id: string;
  userId: string;
  expertUserId: string;
  openedAt: string;
  closedAt?: string | null;
  writable: boolean;
}

export interface ConversationHistoryItem {
  id: string;
  userId: string;
  expertUserId: string;
  openedAt: string;
  closedAt?: string | null;
  writable: boolean;
}

export type MessageType = "TEXT" | "SYSTEM";

export interface ExpertChatMessage {
  id: string;
  conversationId: string;
  senderId: string;
  messageType: MessageType;
  content: string;
  read: boolean;
  readAt?: string | null;
  createdAt: string;
}
