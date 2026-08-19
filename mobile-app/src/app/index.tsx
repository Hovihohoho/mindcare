import { Redirect } from 'expo-router';
import { useAuth } from '@/features/auth/auth-context';
import { SessionLoader } from '@/features/auth/session-loader';

export default function Index() {
  const { status } = useAuth();
  if (status === 'loading') return <SessionLoader />;
  return <Redirect href={status === 'authenticated' ? '/(tabs)/journal' : '/(auth)/login'} />;
}
