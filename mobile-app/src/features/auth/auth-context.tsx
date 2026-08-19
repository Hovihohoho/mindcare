import { createContext, type PropsWithChildren, useCallback, useContext, useEffect, useMemo, useState } from 'react';

import { authService } from '@/services/auth/auth.service';
import { sessionStorage } from '@/services/auth/session.storage';
import type { AuthSession, AuthStatus, LoginInput, RegisterInput } from './auth.types';

type AuthContextValue = {
  session: AuthSession | null;
  status: AuthStatus;
  login(input: LoginInput): Promise<void>;
  register(input: RegisterInput): Promise<void>;
  verifyEmail(email: string, code: string): Promise<void>;
  resendVerification(email: string): Promise<void>;
  logout(): Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [status, setStatus] = useState<AuthStatus>('loading');

  useEffect(() => {
    let mounted = true;
    sessionStorage.load()
      .then(async (storedSession) => {
        if (!mounted) return;
        if (!storedSession) {
          setStatus('unauthenticated');
          return;
        }
        try {
          const user = await authService.me(storedSession.accessToken);
          if (!mounted) return;
          const refreshedSession = { ...storedSession, user };
          await sessionStorage.save(refreshedSession);
          setSession(refreshedSession);
          setStatus('authenticated');
        } catch (error) {
          if (!mounted) return;
          if (error instanceof Error && 'status' in error && [400, 401, 403].includes(Number(error.status))) {
            await sessionStorage.clear();
            setSession(null);
            setStatus('unauthenticated');
          } else {
            setSession(storedSession);
            setStatus('authenticated');
          }
        }
      })
      .catch(() => {
        if (!mounted) return;
        setSession(null);
        setStatus('unauthenticated');
      });
    return () => { mounted = false; };
  }, []);

  const login = useCallback(async (input: LoginInput) => {
    const nextSession = await authService.login(input);
    await sessionStorage.save(nextSession);
    setSession(nextSession);
    setStatus('authenticated');
  }, []);

  const register = useCallback(async (input: RegisterInput) => {
    await authService.register(input);
  }, []);

  const verifyEmail = useCallback((email: string, code: string) => authService.verifyEmail(email, code), []);
  const resendVerification = useCallback((email: string) => authService.resendVerification(email), []);

  const logout = useCallback(async () => {
    try {
      await authService.logout(session?.accessToken);
    } finally {
      try {
        await sessionStorage.clear();
      } finally {
        setSession(null);
        setStatus('unauthenticated');
      }
    }
  }, [session?.accessToken]);

  const value = useMemo(() => ({ session, status, login, register, verifyEmail, resendVerification, logout }), [login, logout, register, resendVerification, session, status, verifyEmail]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
