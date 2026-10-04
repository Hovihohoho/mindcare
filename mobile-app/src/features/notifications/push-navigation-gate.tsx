import type { NotificationResponse } from 'expo-notifications';
import { type Href, useRouter } from 'expo-router';
import { useEffect } from 'react';
import { canUseLocalNotifications, loadNotifications } from './push-registration';

export function PushNavigationGate() {
  const router = useRouter();
  useEffect(() => {
    if (!canUseLocalNotifications()) return;

    let active = true;
    let subscription: { remove(): void } | undefined;
    const open = (response: NotificationResponse | null) => {
      const url = response?.notification.request.content.data?.url;
      if (url === '/emotion') {
        router.navigate({ pathname: '/(tabs)/journal', params: { checkIn: 'true' } });
        return;
      }
      const mobileUrl = url === '/emotion'
        ? '/(tabs)/journal'
        : url === '/care-plan'
          ? '/(tabs)/progress'
          : url === '/health-connect'
            ? '/(tabs)/health-connect'
            : url;
      if (typeof mobileUrl === 'string' && mobileUrl.startsWith('/')) router.navigate(mobileUrl as Href);
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
