import type { Href } from 'expo-router';

// Both Expo responses and inbox actionUrl use this allowlist of mobile destinations.
export function notificationDestination(url: unknown): Href | null {
  if (url === '/emotion') return { pathname: '/(tabs)/journal', params: { checkIn: 'true' } };
  if (url === '/care-plan') return '/(tabs)/progress';
  if (url === '/health-connect') return '/(tabs)/health-connect';
  const routes = ['journal', 'journal-history', 'progress', 'health-connect', 'ai', 'assessments', 'experts', 'settings', 'reminders', 'data-rights'];
  if (typeof url !== 'string') return null;
  const tab = routes.find((route) => url === `/${route}`);
  if (tab) return `/(tabs)/${tab}` as Href;
  if (routes.some((route) => url === `/(tabs)/${route}`)) return url as Href;
  if (['/assessments', '/assessment-history', '/notifications'].includes(url)) return url as Href;
  if (/^\/(journal|assessment-results)\/[a-zA-Z0-9-]+$/.test(url)) return url as Href;
  if (/^\/assessment\/[a-zA-Z0-9-]+$/.test(url)) return url as Href;
  return null;
}
