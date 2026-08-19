import { useCallback, useEffect, useState } from 'react';

import { useAuth } from '@/features/auth/auth-context';
import { ApiClientError } from '@/services/api/api.client';
import { emotionService } from '@/services/emotion/emotion.service';
import type { EmotionJournal, EmotionTrendPoint } from './emotion.types';

function range(days: number) {
  const to = new Date();
  const from = new Date(to);
  from.setHours(0, 0, 0, 0);
  from.setDate(from.getDate() - (days - 1));
  return { from: from.toISOString(), to: to.toISOString() };
}

export function useEmotionJournal(days = 7, limit = 30, includeTrends = true) {
  const { session, logout } = useAuth();
  const token = session?.accessToken;
  const [entries, setEntries] = useState<EmotionJournal[]>([]);
  const [trends, setTrends] = useState<EmotionTrendPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async (refresh = false) => {
    if (!token) return;
    if (refresh) setRefreshing(true);
    else setLoading(true);
    setError('');
    try {
      // Recalculate the upper bound for every request. A journal created after
      // the screen mounted would otherwise be newer than a memoized `to` date
      // and would not appear after pull-to-refresh.
      const dates = range(days);
      const [history, trendData] = await Promise.all([
        emotionService.history(token, dates.from, dates.to, limit),
        includeTrends ? emotionService.trends(token, dates.from, dates.to) : Promise.resolve([]),
      ]);
      setEntries(history.items);
      setTrends(trendData);
    } catch (caught) {
      if (caught instanceof ApiClientError && [401, 403].includes(caught.status ?? 0)) {
        void logout().catch(() => undefined);
      }
      setError(caught instanceof Error ? caught.message : 'Không thể tải dữ liệu cảm xúc.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [days, includeTrends, limit, logout, token]);

  useEffect(() => { void load(); }, [load]);
  return { entries, trends, loading, refreshing, error, reload: load, token };
}
