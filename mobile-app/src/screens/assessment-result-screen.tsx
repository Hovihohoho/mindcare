import { Redirect, useLocalSearchParams, useRouter } from 'expo-router';
import { AppScreen } from '@/components/app-screen';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { useAuthenticatedDetail } from '@/hooks/use-authenticated-detail';
import { assessmentService } from '@/services/assessment/assessment.service';
import { AssessmentResultView } from './assessment-process-screen';

export default function AssessmentResultScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const { data, error, loading, status, reload } = useAuthenticatedDetail(id, assessmentService.result);
  if (status === 'unauthenticated') return <Redirect href="/(auth)/login" />;
  if (data) return <AssessmentResultView result={data} onBack={() => router.back()} />;
  return <AppScreen>{loading ? <SkeletonList rows={3} /> : <DataFeedback kind="error" title="Không thể mở kết quả" description={error} actionLabel="Thử lại" onAction={() => void reload()} />}</AppScreen>;
}
