import { useCallback, useEffect, useState } from 'react';

import { useAuth } from '@/features/auth/auth-context';
import { ApiClientError } from '@/services/api/api.client';

export function useAuthenticatedList<T>(request: (token: string) => Promise<T[]>) {
  const { session, logout } = useAuth();
  const token = session?.accessToken;
  const [data, setData] = useState<T[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async (refresh = false) => {
    if (!token) return;
    if (refresh) setRefreshing(true);
    else setLoading(true);
    setError('');
    try {
      setData(await request(token));
    } catch (caught) {
      if (caught instanceof ApiClientError && [401, 403].includes(caught.status ?? 0)) {
        void logout().catch(() => undefined);
      }
      setError(caught instanceof Error ? caught.message : 'Không thể tải dữ liệu.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [logout, request, token]);

  useEffect(() => { void load(); }, [load]);
  return { data, error, loading, refreshing, reload: load, token };
}
