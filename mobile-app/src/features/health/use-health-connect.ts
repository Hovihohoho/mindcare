import { useFocusEffect } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import { Platform } from 'react-native';

import { useAuth } from '@/features/auth/auth-context';
import { EMPTY_HEALTH_PERMISSIONS } from './health.constants';
import { HealthConnectError, healthConnectService } from './health-connect.service';
import { updateHealthBackgroundRegistration } from './health-background.task';
import { syncHealthIfDue } from './health-sync.coordinator';
import type { HealthConnectSnapshot, HealthMetricSyncItem, HealthSyncResult, HealthTrendPoint } from './health.types';
import { healthApi } from './health.api';
import { healthSyncStorage } from './health.storage';

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
  const [action, setAction] = useState<'background' | 'connect' | 'delete' | 'sync' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [syncResult, setSyncResult] = useState<HealthSyncResult | null>(null);
  const [statistics, setStatistics] = useState<Record<string, number>>({});
  const [trends, setTrends] = useState<Partial<Record<HealthMetricSyncItem['metricType'], HealthTrendPoint[]>>>({});

  const loadStatistics = useCallback(async () => {
    if (!session) return;
    const to = new Date().toISOString();
    const from = new Date(Date.now() - 7 * 86_400_000).toISOString();
    const types = ['STEP_COUNT', 'HEART_RATE', 'SPO2', 'SLEEP_SESSION', 'EXERCISE_SESSION'] as const;
    const results = await Promise.allSettled(types.map((metric) => healthApi.trends(session.accessToken, metric, from, to)));
    const next: Record<string, number> = {};
    const nextTrends: Partial<Record<HealthMetricSyncItem['metricType'], HealthTrendPoint[]>> = {};
    results.forEach((result, index) => {
      if (result.status !== 'fulfilled') return;
      const points = result.value;
      nextTrends[types[index]] = points;
      next[types[index]] = types[index] === 'HEART_RATE' || types[index] === 'SPO2'
        ? points.reduce((sum, point) => sum + point.value * point.count, 0) / Math.max(1, points.reduce((sum, point) => sum + point.count, 0))
        : points.reduce((sum, point) => sum + point.value, 0);
    });
    setStatistics(next);
    setTrends(nextTrends);
  }, [session]);

  const refresh = useCallback(async () => {
    if (!session) return;
    setError(null);
    try {
      setSnapshot(await healthConnectService.getSnapshot(session.user.id));
      await loadStatistics();
    } catch (nextError) {
      setError(messageFor(nextError));
    } finally {
      setLoading(false);
    }
  }, [loadStatistics, session]);

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
      if (permissions.steps || permissions.sleep || permissions.heartRate || permissions.restingHeartRate || permissions.oxygenSaturation || permissions.exercise) {
        await healthApi.enableSource(session.accessToken);
        const result = await syncHealthIfDue(session.user.id, session.accessToken, { force: true });
        setSyncResult(result);
        if (result) setSnapshot((current) => ({ ...current, lastSyncTime: result.syncedAt }));
        await loadStatistics();
      }
    } catch (nextError) {
      setError(messageFor(nextError));
      await refresh();
    } finally {
      setAction(null);
    }
  }, [loadStatistics, refresh, session]);

  const enableBackground = useCallback(async () => {
    setAction('background');
    setError(null);
    try {
      const permissions = await healthConnectService.requestBackgroundPermission();
      setSnapshot((current) => ({ ...current, permissions }));
      await updateHealthBackgroundRegistration(permissions.background);
    } catch (nextError) {
      setError(messageFor(nextError));
      await refresh();
    } finally {
      setAction(null);
    }
  }, [refresh]);

  const sync = useCallback(async () => {
    if (!session) return;
    setAction('sync');
    setError(null);
    setSyncResult(null);
    try {
      const result = await syncHealthIfDue(session.user.id, session.accessToken, { force: true });
      if (!result) return;
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

  const deleteStoredData = useCallback(async () => {
    if (!session) return;
    setAction('delete');
    setError(null);
    try {
      await healthApi.deleteSourceData(session.accessToken);
      await updateHealthBackgroundRegistration(false);
      await healthSyncStorage.clearLastSyncTime(session.user.id);
      setSyncResult(null);
      setStatistics({});
      setTrends({});
      setSnapshot((current) => ({ ...current, lastSyncTime: null }));
    } catch (nextError) {
      setError(messageFor(nextError));
      throw nextError;
    } finally {
      setAction(null);
    }
  }, [session]);

  const connectionStatus = useMemo(() => {
    if (snapshot.availability !== 'available') return 'unavailable' as const;
    const values = [snapshot.permissions.steps, snapshot.permissions.sleep, snapshot.permissions.heartRate,
      snapshot.permissions.restingHeartRate, snapshot.permissions.oxygenSaturation, snapshot.permissions.exercise];
    if (values.every(Boolean)) return 'connected' as const;
    if (values.some(Boolean)) return 'partial' as const;
    return 'disconnected' as const;
  }, [snapshot]);

  return {
    action,
    connect,
    enableBackground,
    connectionStatus,
    deleteStoredData,
    error,
    loading,
    managePermissions: healthConnectService.openHealthConnectSettings,
    openStore: healthConnectService.openHealthConnectStore,
    refresh,
    snapshot,
    statistics,
    sync,
    syncResult,
    trends,
  };
}
