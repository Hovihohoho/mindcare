import Constants from 'expo-constants';
import { Platform } from 'react-native';

function developmentApiUrl() {
  if (Platform.OS === 'web') return 'http://localhost:8079';
  const metroHost = Constants.expoConfig?.hostUri?.split(':')[0];
  return metroHost ? `http://${metroHost}:8079` : 'http://localhost:8079';
}

export const API_URL = (process.env.EXPO_PUBLIC_API_URL?.trim() || developmentApiUrl()).replace(/\/$/, '');
