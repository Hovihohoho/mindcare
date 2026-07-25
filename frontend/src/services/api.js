const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export async function api(path, options = {}) {
  const token = localStorage.getItem('mindcare_token');
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
  });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.message || 'Không thể kết nối máy chủ');
  return body;
}

export const authApi = {
  login: (data) => api('/api/auth/login', { method: 'POST', body: JSON.stringify(data) }),
  register: (data) => api('/api/auth/register', { method: 'POST', body: JSON.stringify(data) }),
  me: () => api('/api/auth/me'),
  verifyEmail: (token) => api(`/api/auth/verify-email?token=${encodeURIComponent(token)}`),
  resendVerification: (email) => api('/api/auth/resend-verification', { method: 'POST', body: JSON.stringify({ email }) }),
};

export const aiApi = {
  chat: (question, topK = 5) => api('/api/ai/chat', { method: 'POST', body: JSON.stringify({ question, topK }) }),
};

export function saveSession(data) {
  localStorage.setItem('mindcare_token', data.accessToken);
  localStorage.setItem('mindcare_user', JSON.stringify(data.user));
}
export function getCurrentUser() {
  try { return JSON.parse(localStorage.getItem('mindcare_user')); } catch { return null; }
}
export function clearSession() {
  localStorage.removeItem('mindcare_token');
  localStorage.removeItem('mindcare_user');
}
