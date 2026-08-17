export interface ChatSource {
  citationNumber: number;
  id?: string;
  title: string;
  sourceUrl: string;
  similarity?: number;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  createdAt: string;
  sources?: ChatSource[];
}
