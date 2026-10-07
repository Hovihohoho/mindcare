import type { NotificationResponse } from 'expo-notifications';
import { useRouter } from 'expo-router';
import { useEffect } from 'react';
import { canUseLocalNotifications, loadNotifications } from './push-registration';
import { notificationDestination } from './notification-navigation';

export function PushNavigationGate() {
  const router = useRouter();
  useEffect(() => {
    if (!canUseLocalNotifications()) return;

    let active = true;
    let subscription: { remove(): void } | undefined;
    const open = (response: NotificationResponse | null) => {
      const url = response?.notification.request.content.data?.url;
      const destination = notificationDestination(url);
      if (destination) router.navigate(destination);
    };

    void loadNotifications()
      .then(async (Notifications) => {
        const lastResponse = await Notifications.getLastNotificationResponseAsync();
        if (!active) return;
        open(lastResponse);
        subscription = Notifications.addNotificationResponseReceivedListener(open);
      })
      .catch(() => undefined);

    return () => {
      active = false;
      subscription?.remove();
    };
  }, [router]);
  return null;
}
