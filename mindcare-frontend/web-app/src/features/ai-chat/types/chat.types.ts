export interface ChatSource {
  citationNumber: number;
  id?: string;
  title: string;
  sourceUrl: string;
  similarity?: number;
}

export interface ChatSafetyDirective {
  level: "NONE" | "CHECK_IN" | "EXPLICIT" | "IMMINENT";
  showSafetyCheck: boolean;
  showEmergencyActions: boolean;
  emergencyNumber?: string | null;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  createdAt: string;
  sources?: ChatSource[];
  safety?: ChatSafetyDirective;
}
