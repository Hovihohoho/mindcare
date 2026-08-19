import { createContext, useContext, useMemo, useState, type PropsWithChildren } from 'react';

export type DataScenario = 'ready' | 'loading' | 'empty' | 'error';

type MockStateValue = {
  scenario: DataScenario;
  setScenario: (scenario: DataScenario) => void;
};

const MockStateContext = createContext<MockStateValue | null>(null);

export function MockStateProvider({ children }: PropsWithChildren) {
  const [scenario, setScenario] = useState<DataScenario>('ready');
  const value = useMemo(() => ({ scenario, setScenario }), [scenario]);
  return <MockStateContext.Provider value={value}>{children}</MockStateContext.Provider>;
}

export function useMockState() {
  const value = useContext(MockStateContext);
  if (!value) throw new Error('useMockState must be used inside MockStateProvider');
  return value;
}
