export interface Conversation {
  id: string;
  bookingId: string;
  openedAt: string;
  closedAt?: string | null;
  writable: boolean;
}

export interface ConversationHistoryItem {
  id: string;
  bookingId: string;
  userId: string;
  expertUserId: string;
  bookingStatus: string;
  startAt: string;
  endAt: string;
  openedAt: string;
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
