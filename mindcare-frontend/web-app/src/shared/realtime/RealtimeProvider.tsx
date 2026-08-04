import { useEffect, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { tokenStorage } from "@/shared/lib/storage";
import {
  realtimeClient,
  type ExpertMessageEvent,
  type NotificationEvent,
} from "./realtimeClient";

export function RealtimeProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [token, setToken] = useState(() => tokenStorage.get());

  useEffect(() => {
    const syncToken = () => setToken(tokenStorage.get());
    window.addEventListener("storage", syncToken);
    window.addEventListener("mindcare:auth-changed", syncToken);
    return () => {
      window.removeEventListener("storage", syncToken);
      window.removeEventListener("mindcare:auth-changed", syncToken);
    };
  }, []);

  useEffect(() => {
    if (token) realtimeClient.connect(token);
    else realtimeClient.disconnect();
    return () => realtimeClient.disconnect();
  }, [token]);

  useEffect(() => {
    const unsubscribeNotifications = realtimeClient.subscribe(
      "notifications",
      (payload) => {
        if ((payload as NotificationEvent).type === "NOTIFICATION_CREATED") {
          queryClient.invalidateQueries({ queryKey: ["notifications"] });
        }
      },
    );
    const unsubscribeExpert = realtimeClient.subscribe("expert", (payload) => {
      const event = payload as ExpertMessageEvent;
      if (event.type !== "EXPERT_MESSAGE_CREATED") return;
      queryClient.invalidateQueries({
        queryKey: ["conversation-messages", event.message.conversationId],
      });
    });
    return () => {
      unsubscribeNotifications();
      unsubscribeExpert();
    };
  }, [queryClient]);

  return children;
}
