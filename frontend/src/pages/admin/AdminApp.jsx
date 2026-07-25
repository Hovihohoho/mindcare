import { Navigate, Route, Routes } from 'react-router-dom';
import AdminLayout from '../../layouts/AdminLayout';
import UserRolePage from './UserRolePage';
import AiDocumentPage from './AiDocumentPage';

export default function AdminApp() {
  return <Routes>
    <Route element={<AdminLayout />}>
      <Route index element={<Navigate to="/users" replace />} />
      <Route path="/users" element={<UserRolePage />} />
      <Route path="/ai-documents" element={<AiDocumentPage />} />
    </Route>
    <Route path="*" element={<Navigate to="/users" replace />} />
  </Routes>;
}
