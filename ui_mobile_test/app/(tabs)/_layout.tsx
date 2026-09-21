import Ionicons from '@expo/vector-icons/Ionicons';
import { Tabs } from 'expo-router';
import { palette } from '../../src/theme/tokens';

const icons = { home: ['home-outline', 'home'], journey: ['pulse-outline', 'pulse'], library: ['book-outline', 'book'], chat: ['chatbubble-ellipses-outline', 'chatbubble-ellipses'], profile: ['person-circle-outline', 'person-circle'] } as const;
export default function TabsLayout() {
  return <Tabs screenOptions={({ route }) => ({ headerShown: false, tabBarActiveTintColor: palette.primary, tabBarInactiveTintColor: palette.muted, tabBarStyle: { backgroundColor: palette.surface, borderTopColor: palette.line, height: 70, paddingTop: 7 }, tabBarLabelStyle: { fontSize: 11, fontWeight: '700' }, tabBarIcon: ({ color, focused, size }) => <Ionicons color={color} name={icons[route.name as keyof typeof icons][focused ? 1 : 0]} size={Math.min(size, 23)} /> })}>
    <Tabs.Screen name="home" options={{ title: 'Hôm nay' }} /><Tabs.Screen name="journey" options={{ title: 'Hành trình' }} /><Tabs.Screen name="library" options={{ title: 'Tự chăm sóc' }} /><Tabs.Screen name="chat" options={{ title: 'Trò chuyện' }} /><Tabs.Screen name="profile" options={{ title: 'Của tôi' }} />
  </Tabs>;
}
