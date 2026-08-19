import Ionicons from '@expo/vector-icons/Ionicons';
import { Redirect, Tabs } from 'expo-router';
import { colors, fonts, type } from '@/theme/tokens';
import { useAuth } from '@/features/auth/auth-context';
import { SessionLoader } from '@/features/auth/session-loader';

const icons = {
  journal: ['heart-outline', 'heart'] as const,
  assessments: ['clipboard-outline', 'clipboard'] as const,
  ai: ['sparkles-outline', 'sparkles'] as const,
  experts: ['people-outline', 'people'] as const,
  settings: ['settings-outline', 'settings'] as const,
};

export default function TabsLayout() {
  const { status } = useAuth();
  if (status === 'loading') return <SessionLoader />;
  if (status === 'unauthenticated') return <Redirect href="/(auth)/login" />;

  return (
    <Tabs
      initialRouteName="journal"
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.brandDark,
        tabBarInactiveTintColor: colors.muted,
        tabBarHideOnKeyboard: true,
        tabBarStyle: { backgroundColor: colors.surface, borderTopColor: colors.line, minHeight: 64 },
        tabBarItemStyle: { minHeight: 52, paddingVertical: 5 },
        tabBarLabelStyle: { fontFamily: fonts.medium, fontSize: type.tab, lineHeight: 14 },
      }}
    >
      {Object.entries({
        journal: 'Nhật ký', assessments: 'Đánh giá', ai: 'AI hỗ trợ', experts: 'Chuyên gia', settings: 'Cài đặt',
      }).map(([name, title]) => (
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
      <Tabs.Screen name="journal-history" options={{ href: null }} />
    </Tabs>
  );
}
