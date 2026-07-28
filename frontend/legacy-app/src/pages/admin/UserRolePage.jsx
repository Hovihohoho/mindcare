import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, message, Modal, Popconfirm, Select, Space, Switch, Table, Tag, Typography } from 'antd';
import axiosClient from '../../services/axiosClient';

export default function UserRolePage() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();

  const loadData = async () => {
    setLoading(true);
    try {
      const [usersResponse, rolesResponse] = await Promise.all([
        axiosClient.get('/api/auth/admin/users'), axiosClient.get('/api/auth/admin/users/roles'),
      ]);
      setUsers(usersResponse.data.data || []);
      setRoles(rolesResponse.data.data || []);
    } catch (error) { message.error(error.response?.data?.message || 'Không tải được dữ liệu quản trị'); }
    finally { setLoading(false); }
  };
  useEffect(() => { loadData(); }, []);

  const showCreate = () => { setEditing(null); form.resetFields(); setOpen(true); };
  const showEdit = (user) => {
    setEditing(user); form.setFieldsValue({ fullName: user.fullName, email: user.email, role: user.role, active: user.active }); setOpen(true);
  };
  const submit = async () => {
    const values = await form.validateFields();
    try {
      if (editing) await axiosClient.put(`/api/auth/admin/users/${editing.id}`, values);
      else await axiosClient.post('/api/auth/admin/users', values);
      message.success(editing ? 'Cập nhật tài khoản thành công' : 'Tạo tài khoản chuyên gia thành công');
      setOpen(false); loadData();
    } catch (error) { message.error(error.response?.data?.message || 'Không thể lưu tài khoản'); }
  };
  const remove = async (id) => {
    try { await axiosClient.delete(`/api/auth/admin/users/${id}`); message.success('Đã xóa tài khoản'); loadData(); }
    catch (error) { message.error(error.response?.data?.message || 'Không thể xóa tài khoản'); }
  };

  const columns = [
    { title: 'Họ tên', dataIndex: 'fullName', sorter: (a, b) => a.fullName.localeCompare(b.fullName) },
    { title: 'Email', dataIndex: 'email' },
    { title: 'Vai trò', dataIndex: 'role', filters: roles.map(r => ({ text: r.name, value: r.name })), onFilter: (v, r) => r.role === v,
      render: role => <Tag color={role === 'ROLE_ADMIN' ? 'red' : role === 'ROLE_EXPERT' ? 'purple' : 'blue'}>{role}</Tag> },
    { title: 'Trạng thái', dataIndex: 'active', render: active => <Tag color={active ? 'green' : 'default'}>{active ? 'Hoạt động' : 'Đã khóa'}</Tag> },
    { title: 'Xác thực email', dataIndex: 'emailVerified', render: verified => <Tag color={verified ? 'green' : 'orange'}>{verified ? 'Đã xác thực' : 'Chờ xác thực'}</Tag> },
    { title: 'Ngày tạo', dataIndex: 'createdAt', render: value => value ? new Date(value).toLocaleDateString('vi-VN') : '-' },
    { title: 'Thao tác', render: (_, user) => <Space><Button onClick={() => showEdit(user)}>Sửa</Button><Popconfirm title="Xóa tài khoản này?" description="Thao tác không thể hoàn tác." onConfirm={() => remove(user.id)}><Button danger>Xóa</Button></Popconfirm></Space> },
  ];

  return <Space direction="vertical" size="large" style={{ width: '100%' }}>
    <div className="page-title"><div><Typography.Title level={2}>Quản lý người dùng & chuyên gia</Typography.Title><Typography.Text type="secondary">Quản lý tài khoản và cấp tài khoản ROLE_EXPERT cho chuyên gia tâm lý.</Typography.Text></div><Button type="primary" onClick={showCreate}>Tạo tài khoản chuyên gia</Button></div>
    <Card><Table rowKey="id" loading={loading} columns={columns} dataSource={users} scroll={{ x: 900 }} /></Card>
    <Modal title={editing ? 'Cập nhật tài khoản' : 'Tạo tài khoản chuyên gia'} open={open} onOk={submit} onCancel={() => setOpen(false)} okText="Lưu" cancelText="Hủy">
      <Form form={form} layout="vertical" initialValues={{ role: 'ROLE_EXPERT', active: true }}>
        <Form.Item label="Họ và tên" name="fullName" rules={[{ required: true, message: 'Nhập họ và tên' }]}><Input /></Form.Item>
        <Form.Item label="Email" name="email" rules={[{ required: true, message: 'Nhập email' }, { type: 'email' }]}><Input /></Form.Item>
        {!editing && <Form.Item label="Mật khẩu tạm" name="password" rules={[{ required: true, min: 8, message: 'Mật khẩu tối thiểu 8 ký tự' }]}><Input.Password /></Form.Item>}
        {editing && <><Form.Item label="Vai trò" name="role" rules={[{ required: true }]}><Select options={roles.map(r => ({ label: r.name, value: r.name }))} /></Form.Item><Form.Item label="Đang hoạt động" name="active" valuePropName="checked"><Switch /></Form.Item></>}
      </Form>
    </Modal>
  </Space>;
}
