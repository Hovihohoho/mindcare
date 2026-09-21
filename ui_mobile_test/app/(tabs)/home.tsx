/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V5 */
import Ionicons from '@expo/vector-icons/Ionicons';
import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Screen, SectionTitle, SoftButton } from '../../src/components/ui';
import { EmotionPicker, EmotionLevel } from '../../src/components/emotion-ui';
import { palette, radius, space, type } from '../../src/theme/tokens';

const metrics = [
  { label: 'Hoạt động', value: '2.480', unit: '/ 6.000 bước', detail: 'Còn 3.520 bước để đạt mục tiêu', icon: 'walk-outline' as const },
  { label: 'Giấc ngủ', value: '7 giờ 12 phút', unit: '', detail: 'Trong khoảng bạn thường duy trì', icon: 'moon-outline' as const },
  { label: 'Nhịp tim nghỉ', value: '64', unit: ' bpm', detail: 'Dữ liệu gần nhất lúc 08:42', icon: 'heart-outline' as const },
  { label: 'Vận động', value: '12', unit: '/ 30 phút', detail: 'Còn 18 phút trong hôm nay', icon: 'timer-outline' as const },
];
export default function HomeScreen() {
  const [mood, setMood] = useState<EmotionLevel>('NEUTRAL');
  return <Screen><ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
    <View style={styles.header}><View><Text style={styles.date}>THỨ NĂM, 11 THÁNG 9</Text><Text style={styles.title}>Hôm nay</Text></View><Pressable accessibilityLabel="Thông báo" style={styles.iconButton}><Ionicons color={palette.ink} name="notifications-outline" size={21} /></Pressable></View>
    <View style={styles.statusLine}><View style={styles.statusDot} /><View style={styles.statusCopy}><Text style={styles.statusTitle}>Dữ liệu sức khỏe đã được cập nhật</Text><Text style={styles.statusText}>Đồng bộ lần cuối 08:42 · Không có chỉ số cần chú ý</Text></View></View>
    <SectionTitle kicker="TÓM TẮT HÔM NAY" title="Chỉ số sức khỏe" action="Xem tất cả" />
    <View style={styles.metrics}>{metrics.map((item, index) => <Metric key={item.label} {...item} last={index === metrics.length - 1} />)}</View>
    <View style={styles.rule} />
    <SectionTitle kicker="CHECK-IN CẢM XÚC" title="Bạn đang cảm thấy thế nào?" />
    <View style={styles.moodPanel}><EmotionPicker value={mood} onChange={setMood} /><Text style={styles.moodNote}>Check-in này chỉ dành cho bạn và giúp bạn nhìn lại xu hướng cảm xúc theo thời gian.</Text></View>
    <View style={styles.rule} />
    <SectionTitle kicker="GỢI Ý PHÙ HỢP" title="Một việc đơn giản tiếp theo" />
    <View style={styles.recommendation}><View style={styles.recommendationIcon}><Ionicons color={palette.primary} name="walk-outline" size={21} /></View><View style={styles.recommendationBody}><Text style={styles.recommendationTitle}>Đi bộ 10 phút</Text><Text style={styles.recommendationText}>Bạn còn 18 phút vận động để hoàn thành mục tiêu hôm nay.</Text><SoftButton label="Bắt đầu hoạt động" icon="arrow-forward" filled /></View></View>
    <Text style={styles.source}>Các chỉ số hoạt động, giấc ngủ và nhịp tim cần được kết nối từ Health Connect.</Text>
  </ScrollView></Screen>;
}

function Metric({ label, value, unit, detail, icon, last }: { label: string; value: string; unit: string; detail: string; icon: keyof typeof Ionicons.glyphMap; last: boolean }) {
  return <View style={[styles.metric, !last && styles.metricRule]}><View style={styles.metricHead}><Text style={styles.metricLabel}>{label}</Text><Ionicons color={palette.muted} name={icon} size={19} /></View><Text style={styles.metricValue}>{value}<Text style={styles.metricUnit}>{unit}</Text></Text><Text style={styles.metricDetail}>{detail}</Text></View>;
}

const styles = StyleSheet.create({
  content: { padding: space.page, paddingBottom: 36 }, header: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between', marginBottom: space.lg }, date: { color: palette.muted, fontSize: type.overline, fontWeight: '700', letterSpacing: .7 }, title: { color: palette.ink, fontSize: type.display, fontWeight: '700', letterSpacing: -.8, marginTop: 4 }, iconButton: { alignItems: 'center', borderColor: palette.line, borderRadius: radius.sm, borderWidth: 1, height: 44, justifyContent: 'center', width: 44 },
  statusLine: { alignItems: 'flex-start', backgroundColor: palette.mint, borderLeftColor: palette.primary, borderLeftWidth: 3, flexDirection: 'row', gap: 10, marginBottom: space.xxl, padding: space.md }, statusDot: { backgroundColor: palette.success, borderRadius: 4, height: 8, marginTop: 5, width: 8 }, statusCopy: { flex: 1 }, statusTitle: { color: palette.ink, fontSize: type.label, fontWeight: '700' }, statusText: { color: palette.muted, fontSize: 12, lineHeight: 18, marginTop: 3 },
  metrics: { backgroundColor: palette.lilac, borderColor: palette.line, borderRadius: radius.md, borderWidth: 1 }, metric: { minHeight: 112, padding: space.md }, metricRule: { borderBottomColor: palette.line, borderBottomWidth: StyleSheet.hairlineWidth }, metricHead: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between' }, metricLabel: { color: palette.ink, fontSize: type.label, fontWeight: '700' }, metricValue: { color: palette.ink, fontSize: 23, fontWeight: '700', letterSpacing: -.4, marginTop: 9 }, metricUnit: { color: palette.muted, fontSize: 14, fontWeight: '500' }, metricDetail: { color: palette.muted, fontSize: 12, marginTop: 5 },
  rule: { backgroundColor: palette.line, height: StyleSheet.hairlineWidth, marginVertical: space.xxl }, moodPanel: { backgroundColor: palette.sky, borderColor: palette.line, borderRadius: radius.md, borderWidth: 1, padding: space.md }, moodNote: { borderTopColor: palette.line, borderTopWidth: StyleSheet.hairlineWidth, color: palette.muted, fontSize: 13, lineHeight: 19, marginTop: space.md, paddingTop: space.md },
  recommendation: { alignItems: 'flex-start', backgroundColor: palette.apricot, borderColor: palette.line, borderLeftColor: palette.apricotDeep, borderLeftWidth: 3, borderRadius: radius.md, borderWidth: 1, flexDirection: 'row', gap: space.md, padding: space.md }, recommendationIcon: { alignItems: 'center', backgroundColor: palette.surface, borderRadius: radius.sm, height: 42, justifyContent: 'center', width: 42 }, recommendationBody: { flex: 1, gap: 7 }, recommendationTitle: { color: palette.ink, fontSize: type.label, fontWeight: '700' }, recommendationText: { color: palette.muted, fontSize: 13, lineHeight: 19, marginBottom: 4 }, source: { color: palette.muted, fontSize: 11, lineHeight: 16, marginTop: space.md },
});
