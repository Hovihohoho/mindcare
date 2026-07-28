import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, message, Modal, Popconfirm, Space, Switch, Table, Tag, Typography } from 'antd';
import axiosClient from '../../services/axiosClient';

export default function AiDocumentPage() {
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form] = Form.useForm();
  const load = async () => { setLoading(true); try { const response = await axiosClient.get('/api/ai/documents'); setDocuments(response.data.data || []); } catch (error) { message.error(error.response?.data?.message || 'Không tải được tài liệu AI'); } finally { setLoading(false); } };
  useEffect(() => { load(); }, []);
  const showCreate = () => { setEditing(null); form.resetFields(); form.setFieldsValue({ active: true, documentType: 'TEXT' }); setOpen(true); };
  const showEdit = document => { setEditing(document); form.setFieldsValue(document); setOpen(true); };
  const submit = async () => { const values = await form.validateFields(); try { if (editing) await axiosClient.put(`/api/ai/documents/${editing.id}`, values); else await axiosClient.post('/api/ai/documents', values); message.success(editing ? 'Cập nhật tài liệu thành công' : 'Nạp tài liệu thành công'); setOpen(false); load(); } catch (error) { message.error(error.response?.data?.message || 'Không thể lưu tài liệu'); } };
  const remove = async id => { try { await axiosClient.delete(`/api/ai/documents/${id}`); message.success('Đã xóa tài liệu'); load(); } catch (error) { message.error(error.response?.data?.message || 'Không thể xóa tài liệu'); } };
  const columns = [
    { title: 'Tiêu đề', dataIndex: 'title' },
    { title: 'Nội dung', dataIndex: 'content', ellipsis: true },
    { title: 'Trạng thái', dataIndex: 'active', render: active => <Tag color={active ? 'green' : 'default'}>{active ? 'Đang dùng' : 'Tạm ẩn'}</Tag> },
    { title: 'Cập nhật', dataIndex: 'updatedAt', render: value => new Date(value).toLocaleString('vi-VN') },
    { title: 'Thao tác', render: (_, document) => <Space><Button onClick={() => showEdit(document)}>Sửa</Button><Popconfirm title="Xóa tài liệu này?" onConfirm={() => remove(document.id)}><Button danger>Xóa</Button></Popconfirm></Space> },
  ];
  return <Space direction="vertical" size="large" style={{ width: '100%' }}>
    <div className="page-title"><div><Typography.Title level={2}>Quản lý tài liệu AI (RAG)</Typography.Title><Typography.Text type="secondary">Nạp nội dung tâm lý học dạng text để chuẩn bị cho bước vector hóa.</Typography.Text></div><Button type="primary" onClick={showCreate}>Nạp tài liệu</Button></div>
    <Card><Table rowKey="id" loading={loading} columns={columns} dataSource={documents} /></Card>
    <Modal width={720} title={editing ? 'Cập nhật tài liệu' : 'Nạp tài liệu AI'} open={open} onOk={submit} onCancel={() => setOpen(false)} okText="Lưu" cancelText="Hủy">
      <Form form={form} layout="vertical"><Form.Item label="Tiêu đề" name="title" rules={[{ required: true, message: 'Nhập tiêu đề' }, { max: 255 }]}><Input /></Form.Item><Form.Item label="Nội dung tài liệu" name="content" rules={[{ required: true, message: 'Nhập nội dung tài liệu' }]}><Input.TextArea rows={12} showCount /></Form.Item><Form.Item label="Nguồn tham khảo (không bắt buộc)" name="sourceUrl"><Input /></Form.Item><Form.Item name="documentType" hidden><Input /></Form.Item><Form.Item label="Đang sử dụng" name="active" valuePropName="checked"><Switch /></Form.Item></Form>
    </Modal>
  </Space>;
}
