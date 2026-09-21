/* Hallmark · pre-emit critique: P5 H4 E5 S4 R5 V4 */
import Ionicons from '@expo/vector-icons/Ionicons';
import { Redirect, Tabs } from 'expo-router';
import { StyleSheet } from 'react-native';
import { colors, fonts, type } from '@/theme/tokens';
import { useAuth } from '@/features/auth/auth-context';
import { SessionLoader } from '@/features/auth/session-loader';

const icons = {
  journal: ['home-outline', 'home'] as const,
  progress: ['pulse-outline', 'pulse'] as const,
  assessments: ['book-outline', 'book'] as const,
  ai: ['chatbubble-ellipses-outline', 'chatbubble-ellipses'] as const,
  settings: ['person-circle-outline', 'person-circle'] as const,
};

const visibleTabs = {
  journal: 'Hôm nay',
  progress: 'Hành trình',
  assessments: 'Tự chăm sóc',
  ai: 'Trò chuyện',
  settings: 'Của tôi',
} as const;

export default function TabsLayout() {
  const { status } = useAuth();
  if (status === 'loading') return <SessionLoader />;
  if (status === 'unauthenticated') return <Redirect href="/(auth)/login" />;

  return (
    <Tabs
      initialRouteName="journal"
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.brand,
        tabBarInactiveTintColor: colors.tertiary,
        tabBarHideOnKeyboard: true,
        tabBarStyle: { backgroundColor: colors.surface, borderTopColor: colors.line, borderTopWidth: StyleSheet.hairlineWidth, height: 70, paddingTop: 7 },
        tabBarItemStyle: { minHeight: 52, paddingBottom: 4, paddingTop: 2 },
        tabBarLabelStyle: { fontFamily: fonts.semibold, fontSize: type.tab, lineHeight: 15 },
      }}
    >
      {Object.entries(visibleTabs).map(([name, title]) => (
        <Tabs.Screen
          key={name}
          name={name}
          options={{
            title,
            tabBarAccessibilityLabel: title,
            tabBarIcon: ({ color, focused, size }) => (
              <Ionicons color={color} name={icons[name as keyof typeof icons][focused ? 1 : 0]} size={Math.min(size, 23)} />
            ),
          }}
        />
      ))}
      <Tabs.Screen name="experts" options={{ href: null }} />
      <Tabs.Screen name="journal-history" options={{ href: null }} />
      <Tabs.Screen name="health-connect" options={{ href: null }} />
      <Tabs.Screen name="data-rights" options={{ href: null }} />
      <Tabs.Screen name="reminders" options={{ href: null }} />
    </Tabs>
  );
}
