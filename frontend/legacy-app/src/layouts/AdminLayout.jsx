import { Button, Layout, Menu, Space, Typography } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

const { Sider, Content, Header } = Layout;
export default function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const user = JSON.parse(localStorage.getItem('mindcare_user') || 'null');
  const logout = () => { localStorage.removeItem('mindcare_token'); localStorage.removeItem('mindcare_user'); window.location.assign('/login'); };
  return <Layout style={{ minHeight: '100vh' }}>
    <Sider breakpoint="lg" collapsedWidth="0">
      <Typography.Title level={4} style={{ color: 'white', padding: 16, margin: 0 }}>MindCare Admin</Typography.Title>
      <Menu theme="dark" mode="inline" selectedKeys={[location.pathname]} items={[
        { key: '/users', label: 'Người dùng & chuyên gia' },
        { key: '/ai-documents', label: 'Tài liệu AI (RAG)' },
      ]} onClick={({ key }) => navigate(key)} />
    </Sider>
    <Layout><Header style={{ background: '#fff', display: 'flex', justifyContent: 'flex-end', paddingInline: 24 }}><Space><Typography.Text>{user?.fullName}</Typography.Text><Button onClick={logout}>Đăng xuất</Button></Space></Header><Content style={{ margin: 24 }}><Outlet /></Content></Layout>
  </Layout>;
}
