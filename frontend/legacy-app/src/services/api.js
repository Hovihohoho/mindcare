const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8079';

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

export const emotionApi = {
  listAssessments: () => api('/api/v1/assessments'),
  getAssessment: (code) => api(`/api/v1/assessments/${encodeURIComponent(code)}`),
  submitAssessment: (code, assessmentVersion, answers) => api(
    `/api/v1/assessments/${encodeURIComponent(code)}/submissions`,
    {
      method: 'POST',
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      body: JSON.stringify({ assessmentVersion, answers }),
    },
  ),
  listJournals: ({ from, to, cursor, limit = 50 }) => {
    const params = new URLSearchParams({ from, to, limit: String(limit) });
    if (cursor) params.set('cursor', cursor);
    return api(`/api/v1/emotion-journals?${params}`);
  },
  getJournal: (id) => api(`/api/v1/emotion-journals/${encodeURIComponent(id)}`),
  createJournal: (data) => api('/api/v1/emotion-journals', {
    method: 'POST',
    body: JSON.stringify(data),
  }),
  deleteJournal: (id) => api(`/api/v1/emotion-journals/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  }),
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
