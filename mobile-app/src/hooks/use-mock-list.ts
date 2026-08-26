import { useCallback, useEffect, useState } from 'react';
import type { DataScenario } from '@/state/mock-state';

type RequestState<T> = {
  key: string;
  data: T[];
  error: boolean;
};

export function useMockList<T>(scenario: DataScenario, request: (scenario: DataScenario) => Promise<T[]>) {
  const [refreshKey, setRefreshKey] = useState(0);
  const currentKey = `${scenario}:${refreshKey}`;
  const [state, setState] = useState<RequestState<T>>({ key: '', data: [], error: false });

  useEffect(() => {
    let active = true;
    if (scenario === 'loading') return () => { active = false; };

    request(scenario)
      .then((data) => active && setState({ key: currentKey, data, error: false }))
      .catch(() => active && setState({ key: currentKey, data: [], error: true }));

    return () => { active = false; };
  }, [currentKey, request, scenario]);

  const retry = useCallback(() => setRefreshKey((key) => key + 1), []);
  const loading = scenario === 'loading' || state.key !== currentKey;
  return { data: loading ? [] : state.data, loading, error: !loading && state.error, retry };
}
