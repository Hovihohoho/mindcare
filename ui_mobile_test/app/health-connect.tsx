import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { DetailScreen, PageIntro, PrimaryAction, Row, Surface } from '../src/components/secondary-ui';
import { palette, space, type } from '../src/theme/tokens';

export default function HealthConnectScreen() {
  const [connected, setConnected] = useState(false);
  return <DetailScreen title="Nguồn sức khỏe"><PageIntro eyebrow="SỨC KHỎE & DỮ LIỆU" title="Kết nối Health Connect" text="Nhập các chỉ số bạn đã cho phép để theo dõi cùng cảm xúc. Bạn luôn có thể ngắt kết nối." />
    <Surface tone="sky"><View style={styles.status}><Text style={styles.statusTitle}>{connected ? 'Đã kết nối' : 'Chưa kết nối'}</Text><Text style={styles.statusText}>{connected ? 'Lần đồng bộ gần nhất: hôm nay, 08:42' : 'MindCare chưa đọc dữ liệu sức khỏe của bạn.'}</Text></View></Surface>
    {connected ? <Surface><Row icon="walk-outline" title="Hoạt động" detail="Bước chân và thời gian vận động" /><Row icon="moon-outline" title="Giấc ngủ" detail="Thời lượng và lịch ngủ" /><Row icon="heart-outline" title="Nhịp tim" detail="Chỉ số nhịp tim nghỉ" last /></Surface> : null}
    <PrimaryAction icon={connected ? 'unlink-outline' : 'link-outline'} label={connected ? 'Ngắt kết nối' : 'Kết nối Health Connect'} onPress={() => setConnected(!connected)} />
    <Text style={styles.note}>Chỉ dữ liệu bạn cho phép mới được hiển thị trong MindCare. Đây là giao diện minh họa, không thực hiện kết nối thật.</Text>
  </DetailScreen>;
}
const styles = StyleSheet.create({ status: { padding: space.md }, statusTitle: { color: palette.ink, fontSize: type.title, fontWeight: '700' }, statusText: { color: palette.muted, fontSize: 13, lineHeight: 19, marginTop: 5 }, note: { color: palette.muted, fontSize: 12, lineHeight: 18, marginTop: space.md } });
