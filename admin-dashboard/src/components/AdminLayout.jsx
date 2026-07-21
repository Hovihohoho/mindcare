import { Button, Layout, Menu, Space, Typography } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

const { Sider, Content, Header } = Layout;

export default function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();

  const logout = () => {
    localStorage.removeItem('jwt_token');
    navigate('/login');
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider breakpoint="lg" collapsedWidth="0">
        <Typography.Title level={4} style={{ color: 'white', padding: 16, margin: 0 }}>
          MindCare Admin
        </Typography.Title>
        <Menu theme="dark" mode="inline" selectedKeys={[location.pathname]}
          items={[{ key: '/admin/users', label: 'Quản lý User/Role' }]}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', display: 'flex', justifyContent: 'flex-end' }}>
          <Space><Button onClick={logout}>Đăng xuất</Button></Space>
        </Header>
        <Content style={{ margin: 16 }}><Outlet /></Content>
      </Layout>
    </Layout>
  );
}
