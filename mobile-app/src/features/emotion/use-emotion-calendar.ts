import { useCallback, useEffect, useMemo, useState } from 'react';

import { useAuth } from '@/features/auth/auth-context';
import { ApiClientError } from '@/services/api/api.client';
import { emotionService } from '@/services/emotion/emotion.service';
import type { EmotionJournal, EmotionTrendPoint } from './emotion.types';

function monthRange(month: Date) {
  const from = new Date(month.getFullYear(), month.getMonth(), 1);
  const to = new Date(month.getFullYear(), month.getMonth() + 1, 1);
  return { from: from.toISOString(), to: to.toISOString() };
}

function dayRange(date: Date) {
  const from = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const to = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1);
  return { from: from.toISOString(), to: to.toISOString() };
}

export function useEmotionCalendar(month: Date, selectedDate: Date) {
  const { session, logout } = useAuth();
  const token = session?.accessToken;
  const monthDates = useMemo(() => monthRange(month), [month]);
  const dayDates = useMemo(() => dayRange(selectedDate), [selectedDate]);
  const [points, setPoints] = useState<EmotionTrendPoint[]>([]);
  const [entries, setEntries] = useState<EmotionJournal[]>([]);
  const [calendarLoading, setCalendarLoading] = useState(true);
  const [dayLoading, setDayLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [calendarError, setCalendarError] = useState('');
  const [dayError, setDayError] = useState('');

  const handleError = useCallback((caught: unknown, fallback: string) => {
    if (caught instanceof ApiClientError && [401, 403].includes(caught.status ?? 0)) {
      void logout().catch(() => undefined);
    }
    return caught instanceof Error ? caught.message : fallback;
  }, [logout]);

  const loadCalendar = useCallback(async (showLoading = true) => {
    if (!token) return;
    if (showLoading) setCalendarLoading(true);
    setCalendarError('');
    try {
      setPoints(await emotionService.trends(token, monthDates.from, monthDates.to));
    } catch (caught) {
      setCalendarError(handleError(caught, 'Không thể tải lịch cảm xúc của tháng này.'));
    } finally {
      if (showLoading) setCalendarLoading(false);
    }
  }, [handleError, monthDates.from, monthDates.to, token]);

  const loadDay = useCallback(async (showLoading = true) => {
    if (!token) return;
    if (showLoading) setDayLoading(true);
    setDayError('');
    try {
      const history = await emotionService.history(token, dayDates.from, dayDates.to, 100);
      setEntries(history.items);
    } catch (caught) {
      setDayError(handleError(caught, 'Không thể tải nhật ký của ngày đã chọn.'));
    } finally {
      if (showLoading) setDayLoading(false);
    }
  }, [dayDates.from, dayDates.to, handleError, token]);

  useEffect(() => { void loadCalendar(); }, [loadCalendar]);
  useEffect(() => { void loadDay(); }, [loadDay]);

  const refresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await Promise.all([loadCalendar(false), loadDay(false)]);
    } finally {
      setRefreshing(false);
    }
  }, [loadCalendar, loadDay]);

  return {
    calendarError, calendarLoading, dayError, dayLoading, entries, points, refresh, refreshing,
    retryCalendar: loadCalendar, retryDay: loadDay,
  };
}
