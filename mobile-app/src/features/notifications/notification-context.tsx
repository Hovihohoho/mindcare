import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { AppState } from 'react-native';
import { useAuth } from '@/features/auth/auth-context';
import { notificationService } from '@/services/notifications/notification.service';
import { canUseLocalNotifications, loadNotifications } from './push-registration';

type NotificationContextValue = { unreadCount: number | null; revision: number; refresh: () => Promise<void>; setCount: (count: number) => void };
const Context = createContext<NotificationContextValue | null>(null);

export function NotificationProvider({ children }: { children: ReactNode }) {
  const { session } = useAuth();
  const token = session?.accessToken;
  const [unreadCount, setUnreadCount] = useState<number | null>(null);
  const [revision, setRevision] = useState(0);
  const generation = useRef(0);
  const setCount = useCallback((count: number) => { generation.current++; setUnreadCount(count); }, []);
  const refresh = useCallback(async () => {
    const request = ++generation.current;
    if (!token) { setUnreadCount(null); return; }
    try {
      const count = await notificationService.getUnreadCount(token);
      if (request === generation.current) { setUnreadCount(count); setRevision((value) => value + 1); }
    } catch (error) {
      if (request === generation.current) setUnreadCount(null);
      throw error;
    }
  }, [token]);
  useEffect(() => {
    setUnreadCount(null);
    const update = () => { void refresh().catch(() => undefined); }; // Badge unavailable on error; inbox displays request errors.
    update();
    const foreground = AppState.addEventListener('change', (state) => { if (state === 'active') update(); });
    let active = true;
    let push: { remove: () => void } | undefined;
    if (token && canUseLocalNotifications()) {
      void loadNotifications().then((notifications) => {
        if (active) push = notifications.addNotificationReceivedListener(update);
      }).catch(() => undefined);
    }
    return () => { active = false; foreground.remove(); push?.remove(); };
  }, [token, refresh]);
  return <Context.Provider value={{ unreadCount, revision, refresh, setCount }}>{children}</Context.Provider>;
}

export function useNotifications() {
  const context = useContext(Context);
  if (!context) throw new Error('NotificationProvider is required');
  return context;
}
