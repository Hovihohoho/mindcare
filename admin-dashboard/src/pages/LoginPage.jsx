import { Button, Form, Input, message, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import axiosClient from '../api/axiosClient';

export default function LoginPage() {
  const navigate = useNavigate();

  const onFinish = async (values) => {
    try {
      const response = await axiosClient.post('/api/auth/login', values);
      localStorage.setItem('jwt_token', response.data.data.accessToken);
      message.success('Đăng nhập thành công');
      navigate('/admin/users');
    } catch (error) {
      message.error(error.response?.data?.message || 'Đăng nhập thất bại');
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: '100px auto', padding: 24 }}>
      <Typography.Title level={2}>MindCare Admin</Typography.Title>
      <Form layout="vertical" onFinish={onFinish}>
        <Form.Item label="Email" name="email"
          rules={[{ required: true, message: 'Vui lòng nhập email' }, { type: 'email' }]}>
          <Input autoComplete="email" />
        </Form.Item>
        <Form.Item label="Mật khẩu" name="password"
          rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}>
          <Input.Password autoComplete="current-password" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block>Đăng nhập</Button>
      </Form>
    </div>
  );
}
