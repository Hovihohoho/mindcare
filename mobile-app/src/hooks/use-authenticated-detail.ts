import { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from '@/features/auth/auth-context';

export function useAuthenticatedDetail<T>(id: string, request: (token: string, id: string) => Promise<T>) {
  const { session, status } = useAuth();
  const token = session?.accessToken;
  const [data, setData] = useState<T>();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const generation = useRef(0);
  const reload = useCallback(async () => {
    if (!token) return;
    const current = ++generation.current;
    setData(undefined);
    setLoading(true);
    setError('');
    try {
      const result = await request(token, id);
      if (generation.current === current) setData(result);
    } catch (caught) {
      if (generation.current === current) setError(caught instanceof Error ? caught.message : 'Không thể tải dữ liệu.');
    } finally {
      if (generation.current === current) setLoading(false);
    }
  }, [id, request, token]);
  useEffect(() => { void reload(); return () => { generation.current++; }; }, [reload]);
  return { data, error, loading, status, reload };
}
