import type { ExpertChatMessage } from "@/features/expert-chat/types/expert-chat.types";
import type { NotificationItem } from "@/features/notification/types/notification.types";
import type { ChatSource } from "@/features/ai-chat/types/chat.types";

type RealtimeChannel = "ai" | "expert" | "notifications";
type Listener = (payload: unknown) => void;

interface AiResponseEvent {
  type: "AI_RESPONSE";
  requestId: string;
  data: { answer: string; sources: ChatSource[] };
}

interface AiErrorEvent {
  type: "AI_ERROR";
  requestId?: string;
  message: string;
}

export interface ExpertMessageEvent {
  type: "EXPERT_MESSAGE_CREATED";
  message: ExpertChatMessage;
}

export interface NotificationEvent {
  type: "NOTIFICATION_CREATED";
  notification: NotificationItem;
}

const paths: Record<RealtimeChannel, string> = {
  ai: "/ws/ai",
  expert: "/ws/expert",
  notifications: "/ws/notifications",
};

class RealtimeClient {
  private sockets = new Map<RealtimeChannel, WebSocket>();
  private listeners = new Map<RealtimeChannel, Set<Listener>>();
  private reconnectAttempts = new Map<RealtimeChannel, number>();
  private reconnectTimers = new Map<RealtimeChannel, number>();
  private token?: string;
  private generation = 0;

  connect(token: string) {
    if (this.token === token && this.sockets.size === 3) return;
    this.disconnect();
    this.token = token;
    this.generation += 1;
    const generation = this.generation;
    (Object.keys(paths) as RealtimeChannel[]).forEach((channel) =>
      this.open(channel, generation),
    );
  }

  disconnect() {
    this.generation += 1;
    this.reconnectTimers.forEach((timer) => window.clearTimeout(timer));
    this.reconnectTimers.clear();
    this.sockets.forEach((socket) => socket.close(1000, "Signed out"));
    this.sockets.clear();
    this.reconnectAttempts.clear();
    this.token = undefined;
  }

  subscribe(channel: RealtimeChannel, listener: Listener) {
    const channelListeners =
      this.listeners.get(channel) ?? new Set<Listener>();
    channelListeners.add(listener);
    this.listeners.set(channel, channelListeners);
    return () => channelListeners.delete(listener);
  }

  isOpen(channel: RealtimeChannel) {
    return this.sockets.get(channel)?.readyState === WebSocket.OPEN;
  }

  askAi(question: string, topK = 5) {
    const socket = this.sockets.get("ai");
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      return Promise.reject(new Error("AI WebSocket is not connected"));
    }
    const requestId = crypto.randomUUID();
    return new Promise<{ answer: string; sources: ChatSource[] }>(
      (resolve, reject) => {
        const timeout = window.setTimeout(() => {
          unsubscribe();
          reject(new Error("AI WebSocket response timed out"));
        }, 90_000);
        const unsubscribe = this.subscribe("ai", (payload) => {
          const event = payload as AiResponseEvent | AiErrorEvent;
          if (event.requestId !== requestId) return;
          window.clearTimeout(timeout);
          unsubscribe();
          if (event.type === "AI_RESPONSE") resolve(event.data);
          else reject(new Error(event.message));
        });
        socket.send(JSON.stringify({
          type: "AI_QUESTION",
          requestId,
          question,
          topK,
        }));
      },
    );
  }

  private open(channel: RealtimeChannel, generation: number) {
    if (!this.token || generation !== this.generation) return;
    const baseUrl = new URL(
      import.meta.env.VITE_API_URL ?? "http://localhost:8079",
    );
    baseUrl.protocol = baseUrl.protocol === "https:" ? "wss:" : "ws:";
    baseUrl.pathname = paths[channel];
    baseUrl.search = "";
    baseUrl.searchParams.set("access_token", this.token);
    const socket = new WebSocket(baseUrl);
    this.sockets.set(channel, socket);

    socket.onopen = () => this.reconnectAttempts.set(channel, 0);
    socket.onmessage = (message) => {
      try {
        const payload = JSON.parse(message.data as string);
        this.listeners.get(channel)?.forEach((listener) => listener(payload));
      } catch {
        // Ignore malformed server frames and keep the connection alive.
      }
    };
    socket.onclose = () => {
      if (generation !== this.generation || !this.token) return;
      const attempt = (this.reconnectAttempts.get(channel) ?? 0) + 1;
      this.reconnectAttempts.set(channel, attempt);
      const delay = Math.min(1_000 * 2 ** (attempt - 1), 15_000);
      const timer = window.setTimeout(
        () => this.open(channel, generation),
        delay,
      );
      this.reconnectTimers.set(channel, timer);
    };
  }
}

export const realtimeClient = new RealtimeClient();
