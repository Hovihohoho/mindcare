import { Redirect, useRouter } from 'expo-router';
import { FlatList, RefreshControl } from 'react-native';
import { AppScreen } from '@/components/app-screen';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { ListItem } from '@/components/list-item';
import { ScreenHeader } from '@/components/screen-header';
import { useAuth } from '@/features/auth/auth-context';
import { useAuthenticatedList } from '@/hooks/use-authenticated-list';
import { assessmentService } from '@/services/assessment/assessment.service';
import { spacing } from '@/theme/tokens';

async function loadHistory(token: string) {
  const to = new Date();
  const from = new Date(to);
  from.setDate(from.getDate() - 365);
  return (await assessmentService.history(token, from.toISOString(), to.toISOString())).items;
}

export default function AssessmentHistoryScreen() {
  const router = useRouter();
  const { status } = useAuth();
  const { data, error, loading, refreshing, reload } = useAuthenticatedList(loadHistory);
  if (status === 'unauthenticated') return <Redirect href="/(auth)/login" />;
  return <AppScreen>
    <ScreenHeader title="Kết quả trong 365 ngày" onBack={() => router.back()} />
    {loading ? <SkeletonList rows={3} /> : error ? <DataFeedback kind="error" title="Chưa tải được lịch sử" description={error} actionLabel="Thử lại" onAction={() => void reload()} /> :
      <FlatList data={data} keyExtractor={(item) => item.resultId} contentContainerStyle={{ padding: spacing.page }}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void reload(true)} />}
        ListEmptyComponent={<DataFeedback kind="empty" title="Chưa có kết quả" description="Chưa có bài đánh giá trong khoảng thời gian này." />}
        renderItem={({ item }) => <ListItem title={item.assessmentCode} description={`${item.totalScore} điểm · ${new Date(item.createdAt).toLocaleString('vi-VN')}`} icon="clipboard-outline"
          onPress={() => router.push({ pathname: '/assessment-results/[id]', params: { id: item.resultId } })} />} />}
  </AppScreen>;
}
