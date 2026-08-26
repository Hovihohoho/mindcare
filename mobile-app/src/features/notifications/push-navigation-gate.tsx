import * as Notifications from 'expo-notifications';
import { type Href, useRouter } from 'expo-router';
import { useEffect } from 'react';

export function PushNavigationGate() {
  const router = useRouter();
  useEffect(() => {
    const open = (response: Notifications.NotificationResponse | null) => {
      const url = response?.notification.request.content.data?.url;
      const mobileUrl = url === '/emotion' ? '/(tabs)/journal' : url === '/care-plan' ? '/(tabs)/progress' : url;
      if (typeof mobileUrl === 'string' && mobileUrl.startsWith('/')) router.push(mobileUrl as Href);
    };
    void Notifications.getLastNotificationResponseAsync().then(open);
    const subscription = Notifications.addNotificationResponseReceivedListener(open);
    return () => subscription.remove();
  }, [router]);
  return null;
}
