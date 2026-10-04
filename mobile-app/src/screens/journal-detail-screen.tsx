import { Redirect, useLocalSearchParams, useRouter } from 'expo-router';
import { ScrollView, StyleSheet, Text } from 'react-native';
import { AppScreen } from '@/components/app-screen';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { ScreenHeader } from '@/components/screen-header';
import { emotionOptionMap } from '@/features/emotion/emotion.constants';
import { useAuthenticatedDetail } from '@/hooks/use-authenticated-detail';
import { emotionService } from '@/services/emotion/emotion.service';
import { colors, fonts, spacing, type } from '@/theme/tokens';

export default function JournalDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const { data, error, loading, status, reload } = useAuthenticatedDetail(id, emotionService.detail);
  if (status === 'unauthenticated') return <Redirect href="/(auth)/login" />;
  return <AppScreen>
    <ScreenHeader title="Chi tiết nhật ký" onBack={() => router.back()} />
    {loading ? <SkeletonList rows={2} /> : !data ? <DataFeedback kind="error" title="Không thể mở nhật ký" description={error} actionLabel="Thử lại" onAction={() => void reload()} /> :
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>{emotionOptionMap[data.emotionType].label}</Text>
        <Text style={styles.date}>{new Date(data.createdAt).toLocaleString('vi-VN')}</Text>
        <Text selectable style={styles.body}>{data.content || 'Không có ghi chú'}</Text>
      </ScrollView>}
  </AppScreen>;
}
const styles = StyleSheet.create({
  content: { padding: spacing.page, gap: spacing.md },
  title: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.section },
  date: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption },
  body: { color: colors.ink, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 24 },
});
