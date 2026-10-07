import Constants from 'expo-constants';
import { Platform } from 'react-native';

function isAndroidStudioEmulator() {
  if (Platform.OS !== 'android') return false;

  const android = Platform.constants as {
    Brand?: string;
    Fingerprint?: string;
    Manufacturer?: string;
    Model?: string;
  };

  return (
    android.Fingerprint?.startsWith('generic') ||
    android.Fingerprint?.startsWith('unknown') ||
    android.Model?.includes('sdk') ||
    android.Model?.includes('Emulator') ||
    android.Manufacturer?.includes('Genymotion') ||
    (android.Brand?.startsWith('generic') && android.Brand?.endsWith('generic'))
  );
}

function developmentApiUrl() {
  if (Platform.OS === 'web') return 'http://localhost:8079';
  // Android Studio's emulator maps 10.0.2.2 to the development machine.
  // Unlike a Wi-Fi address, it remains stable when the host changes networks.
  if (isAndroidStudioEmulator()) return 'http://10.0.2.2:8079';
  const metroHost = Constants.expoConfig?.hostUri?.split(':')[0];
  return metroHost ? `http://${metroHost}:8079` : 'http://localhost:8079';
}

const configuredApiUrl = process.env.EXPO_PUBLIC_API_URL?.trim();
if (!__DEV__ && !configuredApiUrl) {
  throw new Error('Configuration error: EXPO_PUBLIC_API_URL is required for production/release builds.');
}

export const API_URL = (configuredApiUrl || developmentApiUrl()).replace(/\/$/, '');
