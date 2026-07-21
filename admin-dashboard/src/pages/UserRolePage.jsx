import { useEffect, useState } from 'react';
import { Button, Card, message, Space, Table, Tag, Typography } from 'antd';
import axiosClient from '../api/axiosClient';

export default function UserRolePage() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const [userResponse, roleResponse] = await Promise.all([
        axiosClient.get('/api/auth/admin/users'),
        axiosClient.get('/api/auth/admin/users/roles'),
      ]);
      setUsers(userResponse.data.data);
      setRoles(roleResponse.data.data);
    } catch (error) {
      message.error(error.response?.data?.message || 'Không tải được dữ liệu quản trị');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  const grantExpert = async (id) => {
    try {
      await axiosClient.patch(`/api/auth/admin/users/${id}/expert`);
      message.success('Đã cấp quyền chuyên gia');
      loadData();
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể cập nhật vai trò');
    }
  };

  const columns = [
    { title: 'Họ tên', dataIndex: 'fullName' },
    { title: 'Email', dataIndex: 'email' },
    { title: 'Vai trò', dataIndex: 'role', render: (role) => <Tag color="blue">{role}</Tag> },
    { title: 'Trạng thái', dataIndex: 'active',
      render: (active) => <Tag color={active ? 'green' : 'red'}>{active ? 'Hoạt động' : 'Đã khóa'}</Tag> },
    { title: 'Thao tác', key: 'action',
      render: (_, user) => user.role === 'ROLE_USER' && (
        <Button onClick={() => grantExpert(user.id)}>Cấp Expert</Button>
      ) },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Typography.Title level={2}>Quản lý User/Role</Typography.Title>
      <Card title="Vai trò hệ thống">
        <Space wrap>{roles.map((role) => <Tag key={role.id}>{role.name}</Tag>)}</Space>
      </Card>
      <Card>
        <Table rowKey="id" loading={loading} columns={columns} dataSource={users} />
      </Card>
    </Space>
  );
}
