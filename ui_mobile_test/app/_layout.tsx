import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

export default function RootLayout() {
  return <><StatusBar style="dark" /><Stack screenOptions={{ headerShown: false, animation: 'fade' }}><Stack.Screen name="(tabs)" /><Stack.Screen name="care/ritual" options={{ presentation: 'modal' }} /><Stack.Screen name="health-connect" /><Stack.Screen name="assessments" /><Stack.Screen name="assessment/[code]" /><Stack.Screen name="progress" /><Stack.Screen name="reminders" /><Stack.Screen name="journal-history" /><Stack.Screen name="data-rights" /><Stack.Screen name="settings" /><Stack.Screen name="auth/login" /><Stack.Screen name="auth/register" /><Stack.Screen name="auth/verify" /></Stack></>;
}
