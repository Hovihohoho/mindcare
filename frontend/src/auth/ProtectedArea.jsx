import { getCurrentUser } from '../services/api';

export default function ProtectedArea({ roles, children }) {
  const user = getCurrentUser();
  const token = localStorage.getItem('mindcare_token');
  if (!token || !user) { window.location.replace('/login'); return null; }
  if (!roles.includes(user.role)) { window.location.replace('/'); return null; }
  return children;
}
