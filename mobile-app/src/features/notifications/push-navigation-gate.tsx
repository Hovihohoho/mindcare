import type { NotificationResponse } from 'expo-notifications';
import { type Href, useRouter } from 'expo-router';
import { useEffect } from 'react';
import { canUsePushNotifications, loadNotifications } from './push-registration';

export function PushNavigationGate() {
  const router = useRouter();
  useEffect(() => {
    if (!canUsePushNotifications()) return;

    let active = true;
    let subscription: { remove(): void } | undefined;
    const open = (response: NotificationResponse | null) => {
      const url = response?.notification.request.content.data?.url;
      const mobileUrl = url === '/emotion' ? '/(tabs)/journal' : url === '/care-plan' ? '/(tabs)/progress' : url;
      if (typeof mobileUrl === 'string' && mobileUrl.startsWith('/')) router.push(mobileUrl as Href);
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
