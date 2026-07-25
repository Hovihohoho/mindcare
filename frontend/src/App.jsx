import { lazy, Suspense, useEffect } from 'react';
import ProtectedArea from './auth/ProtectedArea';
import userStyles from './pages/user/user.css?inline';
import expertStyles from './pages/expert/expert.css?inline';
import adminStyles from './pages/admin/admin.css?inline';

const UserApp = lazy(() => import('./pages/user/UserApp'));
const ExpertApp = lazy(() => import('./pages/expert/ExpertApp'));
const AdminApp = lazy(() => import('./pages/admin/AdminApp'));

function AreaStyles({ css }) {
  useEffect(() => {
    const style = document.createElement('style');
    style.dataset.mindcareArea = 'true';
    style.textContent = css;
    document.head.appendChild(style);
    return () => style.remove();
  }, [css]);
  return null;
}

export default function App({ area }) {
  if (area === 'admin') return <><AreaStyles css={adminStyles}/><ProtectedArea roles={['ROLE_ADMIN']}><Suspense fallback={null}><AdminApp/></Suspense></ProtectedArea></>;
  if (area === 'expert') return <><AreaStyles css={expertStyles}/><ProtectedArea roles={['ROLE_EXPERT']}><Suspense fallback={null}><ExpertApp/></Suspense></ProtectedArea></>;
  return <><AreaStyles css={userStyles}/><Suspense fallback={null}><UserApp/></Suspense></>;
}
