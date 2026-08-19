import type { Assessment, ChatMessage, Expert, JournalEntry, SettingItem } from '@/types/mindcare';

export const journalEntries: JournalEntry[] = [
  { id: 'j1', title: 'Bình yên', note: 'Mình đã dành một buổi sáng chậm rãi và hoàn thành việc quan trọng nhất.', createdAt: 'Hôm nay, 08:30', tone: 'calm', score: 4 },
  { id: 'j2', title: 'Hơi lo lắng', note: 'Cuộc họp chiều nay khiến mình căng thẳng. Đi bộ 15 phút đã giúp dễ chịu hơn.', createdAt: 'Hôm qua, 20:15', tone: 'anxious', score: 2 },
  { id: 'j3', title: 'Vui vẻ', note: 'Một cuộc trò chuyện ngắn với bạn thân làm cả ngày trở nên nhẹ nhàng.', createdAt: '09 thg 8, 19:40', tone: 'happy', score: 5 },
  { id: 'j4', title: 'Mệt mỏi', note: 'Mình cần ngủ sớm và giảm bớt những việc không thật sự cần thiết.', createdAt: '08 thg 8, 22:10', tone: 'tired', score: 2 },
];

export const assessments: Assessment[] = [
  { id: 'a1', title: 'Sàng lọc lo âu GAD-7', description: 'Quan sát mức độ lo âu trong 2 tuần gần đây.', duration: '3–5 phút', questionCount: 7, category: 'Lo âu' },
  { id: 'a2', title: 'Sàng lọc trầm cảm PHQ-9', description: 'Nhận diện sớm các dấu hiệu cần được quan tâm.', duration: '5 phút', questionCount: 9, category: 'Tâm trạng', completed: true },
  { id: 'a3', title: 'Mức độ căng thẳng PSS-10', description: 'Đánh giá cách bạn cảm nhận và ứng phó với áp lực.', duration: '5–7 phút', questionCount: 10, category: 'Căng thẳng' },
  { id: 'a4', title: 'Chất lượng giấc ngủ', description: 'Xem lại nhịp ngủ và những yếu tố đang ảnh hưởng.', duration: '4 phút', questionCount: 8, category: 'Giấc ngủ' },
];

export const chatMessages: ChatMessage[] = [
  { id: 'm1', role: 'assistant', text: 'Chào bạn, hôm nay bạn muốn cùng mình gỡ rối điều gì?', createdAt: '09:20' },
  { id: 'm2', role: 'user', text: 'Mình thấy khó tập trung và có quá nhiều việc cần làm.', createdAt: '09:21' },
  { id: 'm3', role: 'assistant', text: 'Mình hiểu. Hãy thử chọn một việc nhỏ nhất có thể hoàn thành trong 10 phút. Việc nào đang hiện lên đầu tiên?', createdAt: '09:21' },
];

export const experts: Expert[] = [
  { id: 'e1', name: 'Nguyễn An Nhiên', role: 'Chuyên gia tham vấn tâm lý', specialties: ['Lo âu', 'Căng thẳng'], experience: 8, availability: 'Trống lúc 19:30', initials: 'AN' },
  { id: 'e2', name: 'Trần Minh Khoa', role: 'Nhà tâm lý học lâm sàng', specialties: ['Trầm cảm', 'Giấc ngủ'], experience: 10, availability: 'Trống ngày mai', initials: 'MK' },
  { id: 'e3', name: 'Lê Hà Vy', role: 'Chuyên gia trị liệu gia đình', specialties: ['Mối quan hệ', 'Gia đình'], experience: 7, availability: 'Trống thứ Năm', initials: 'HV' },
  { id: 'e4', name: 'Phạm Gia Linh', role: 'Chuyên gia tham vấn', specialties: ['Học tập', 'Định hướng'], experience: 6, availability: 'Trống lúc 20:00', initials: 'GL' },
];

export const settings: SettingItem[] = [
  { id: 's1', title: 'Hồ sơ cá nhân', description: 'Tên hiển thị và thông tin liên hệ', icon: 'person' },
  { id: 's2', title: 'Thông báo', description: 'Nhắc nhật ký và lịch hẹn', icon: 'notifications' },
  { id: 's3', title: 'Quyền riêng tư', description: 'Dữ liệu sức khỏe và quyền truy cập', icon: 'lock' },
  { id: 's4', title: 'Ngôn ngữ', description: 'Tiếng Việt', icon: 'language' },
  { id: 's5', title: 'Bảo mật tài khoản', description: 'Mật khẩu và phiên đăng nhập', icon: 'shield' },
];
