import { Redirect, Stack } from 'expo-router';

import { useAuth } from '@/features/auth/auth-context';
import { SessionLoader } from '@/features/auth/session-loader';

export default function AuthLayout() {
  const { status } = useAuth();
  if (status === 'loading') return <SessionLoader />;
  if (status === 'authenticated') return <Redirect href="/(tabs)/journal" />;
  return <Stack screenOptions={{ headerShown: false, animation: 'fade' }} />;
}
