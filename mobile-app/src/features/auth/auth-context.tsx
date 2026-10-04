import { createContext, type PropsWithChildren, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';

import { authService } from '@/services/auth/auth.service';
import { sessionStorage } from '@/services/auth/session.storage';
import { onUnauthorized } from '@/services/api/api.client';
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
  const activeToken = useRef<string | null>(null);
  const invalidation = useRef<Promise<void>>(Promise.resolve());

  useEffect(() => onUnauthorized((token) => {
    // A late response from an older session must not log out a new login.
    if (activeToken.current !== token) return;
    activeToken.current = null;
    setSession(null);
    setStatus('unauthenticated');
    invalidation.current = sessionStorage.clear().catch(() => undefined);
  }), []);

  useEffect(() => {
    let mounted = true;
    sessionStorage.load()
      .then(async (storedSession) => {
        if (!mounted) return;
        if (!storedSession) {
          setStatus('unauthenticated');
          return;
        }
        activeToken.current = storedSession.accessToken;
        try {
          const user = await authService.me(storedSession.accessToken);
          if (!mounted || activeToken.current !== storedSession.accessToken) return;
          const refreshedSession = { ...storedSession, user };
          await sessionStorage.save(refreshedSession);
          if (!mounted || activeToken.current !== storedSession.accessToken) return;
          setSession(refreshedSession);
          setStatus('authenticated');
        } catch (error) {
          if (!mounted) return;
          if (activeToken.current !== storedSession.accessToken) return;
          if (error instanceof Error && 'status' in error && [400, 401, 403].includes(Number(error.status))) {
            await sessionStorage.clear();
            activeToken.current = null;
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
    await invalidation.current;
    await sessionStorage.save(nextSession);
    activeToken.current = nextSession.accessToken;
    setSession(nextSession);
    setStatus('authenticated');
  }, []);

  const register = useCallback(async (input: RegisterInput) => {
    await authService.register(input);
  }, []);

  const verifyEmail = useCallback((email: string, code: string) => authService.verifyEmail(email, code), []);
  const resendVerification = useCallback((email: string) => authService.resendVerification(email), []);

  const logout = useCallback(async () => {
    const token = activeToken.current;
    try {
      await authService.logout(token ?? undefined);
    } finally {
      if (activeToken.current === null || activeToken.current === token) {
        activeToken.current = null;
        try {
          await invalidation.current;
          await sessionStorage.clear();
        } finally {
          setSession(null);
          setStatus('unauthenticated');
        }
      }
    }
  }, []);

  const value = useMemo(() => ({ session, status, login, register, verifyEmail, resendVerification, logout }), [login, logout, register, resendVerification, session, status, verifyEmail]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
