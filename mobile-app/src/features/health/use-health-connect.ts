import { useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { Platform } from 'react-native';

import { useAuth } from '@/features/auth/auth-context';
import { EMPTY_HEALTH_PERMISSIONS } from './health.constants';
import { HealthConnectError, healthConnectService } from './health-connect.service';
import type { HealthConnectSnapshot, HealthSyncResult } from './health.types';

const INITIAL_SNAPSHOT: HealthConnectSnapshot = {
  availability: Platform.OS === 'android' ? 'unavailable' : 'unsupported-platform',
  initialized: false,
  lastSyncTime: null,
  permissions: { ...EMPTY_HEALTH_PERMISSIONS },
};

function messageFor(error: unknown) {
  if (error instanceof HealthConnectError) return error.message;
  if (error instanceof Error) return error.message;
  return 'Đã xảy ra lỗi khi làm việc với Health Connect.';
}

export function useHealthConnect() {
  const { session } = useAuth();
  const [snapshot, setSnapshot] = useState(INITIAL_SNAPSHOT);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<'connect' | 'sync' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [syncResult, setSyncResult] = useState<HealthSyncResult | null>(null);

  const refresh = useCallback(async () => {
    if (!session) return;
    setError(null);
    try {
      setSnapshot(await healthConnectService.getSnapshot(session.user.id));
    } catch (nextError) {
      setError(messageFor(nextError));
    } finally {
      setLoading(false);
    }
  }, [session]);

  useFocusEffect(useCallback(() => {
    void refresh();
  }, [refresh]));

  const connect = useCallback(async () => {
    if (!session) return;
    setAction('connect');
    setError(null);
    try {
      const permissions = await healthConnectService.requestHealthPermissions();
      setSnapshot((current) => ({ ...current, availability: 'available', initialized: true, permissions }));
    } catch (nextError) {
      setError(messageFor(nextError));
      await refresh();
    } finally {
      setAction(null);
    }
  }, [refresh, session]);

  const sync = useCallback(async () => {
    if (!session) return;
    setAction('sync');
    setError(null);
    setSyncResult(null);
    try {
      const result = await healthConnectService.syncHealthData(session.user.id, session.accessToken);
      setSyncResult(result);
      setSnapshot((current) => ({ ...current, lastSyncTime: result.syncedAt }));
      await refresh();
    } catch (nextError) {
      setError(messageFor(nextError));
      await refresh();
    } finally {
      setAction(null);
    }
  }, [refresh, session]);

  const connectionStatus = useMemo(() => {
    if (snapshot.availability !== 'available') return 'unavailable' as const;
    const values = Object.values(snapshot.permissions);
    if (values.every(Boolean)) return 'connected' as const;
    if (values.some(Boolean)) return 'partial' as const;
    return 'disconnected' as const;
  }, [snapshot]);

  return {
    action,
    connect,
    connectionStatus,
    error,
    loading,
    managePermissions: healthConnectService.openHealthConnectSettings,
    openStore: healthConnectService.openHealthConnectStore,
    refresh,
    snapshot,
    sync,
    syncResult,
  };
}

