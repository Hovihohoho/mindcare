import Ionicons from '@expo/vector-icons/Ionicons';
import { Redirect, useLocalSearchParams, useRouter } from 'expo-router';
import { useCallback, useEffect, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';

import { AppScreen } from '@/components/app-screen';
import { ActionButton } from '@/components/buttons';
import { DataFeedback, SkeletonList } from '@/components/data-states';
import { useAuth } from '@/features/auth/auth-context';
import { ApiClientError } from '@/services/api/api.client';
import { assessmentService, type AssessmentDetail, type AssessmentResult } from '@/services/assessment/assessment.service';
import { colors, fonts, radius, shadows, spacing, type } from '@/theme/tokens';

export default function AssessmentProcessScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ code: string }>();
  const code = Array.isArray(params.code) ? params.code[0] : params.code;
  const { session, status, logout } = useAuth();
  const [assessment, setAssessment] = useState<AssessmentDetail>();
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [index, setIndex] = useState(0);
  const [result, setResult] = useState<AssessmentResult>();
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    if (!session?.accessToken || !code) return;
    setLoading(true);
    setError('');
    try {
      setAssessment(await assessmentService.detail(session.accessToken, code));
    } catch (caught) {
      if (caught instanceof ApiClientError && [401, 403].includes(caught.status ?? 0)) void logout().catch(() => undefined);
      setError(caught instanceof Error ? caught.message : 'Không thể tải bài đánh giá.');
    } finally {
      setLoading(false);
    }
  }, [code, logout, session?.accessToken]);

  useEffect(() => { void load(); }, [load]);

  const question = assessment?.questions[index];
  const submit = async () => {
    if (!assessment || !session?.accessToken || submitting) return;
    setSubmitting(true);
    setError('');
    try {
      const payload = assessment.questions.map((item) => ({ questionId: item.id, optionId: answers[item.id] }));
      setResult(await assessmentService.submit(session.accessToken, assessment.code, assessment.assessmentVersion, payload));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Không thể gửi bài đánh giá.');
    } finally {
      setSubmitting(false);
    }
  };

  if (status === 'unauthenticated') return <Redirect href="/(auth)/login" />;
  if (loading) return <AppScreen><SkeletonList rows={3} /></AppScreen>;
  if (!assessment || error && !question) {
    return <AppScreen><DataFeedback actionLabel="Thử lại" description={error || 'Bài đánh giá không tồn tại.'} kind="error" onAction={() => void load()} title="Không thể mở bài đánh giá" /></AppScreen>;
  }

  if (result) {
    return (
      <AppScreen>
        <FlatList
          contentContainerStyle={styles.resultContent}
          data={result.recommendations}
          keyExtractor={(item, itemIndex) => `${itemIndex}-${item}`}
          ListHeaderComponent={(
            <View>
              <BackButton onPress={() => router.back()} />
              <View style={styles.resultCard}>
                <View style={styles.resultIcon}><Ionicons color={colors.brandDark} name="checkmark-circle-outline" size={30} /></View>
                <Text style={styles.resultEyebrow}>Kết quả {result.assessmentCode}</Text>
                <Text style={styles.score}>{result.totalScore} điểm</Text>
                <Text style={styles.risk}>{riskLabel(result.riskLevel)}</Text>
                <Text style={styles.notice}>{result.screeningNotice}</Text>
              </View>
              <Text style={styles.recommendationTitle}>Gợi ý dành cho bạn</Text>
            </View>
          )}
          renderItem={({ item }) => <View style={styles.recommendation}><Ionicons color={colors.mintInk} name="leaf-outline" size={19} /><Text style={styles.recommendationText}>{item}</Text></View>}
        />
      </AppScreen>
    );
  }

  if (!question) return null;
  const selected = answers[question.id];
  const last = index === assessment.questions.length - 1;
  return (
    <AppScreen>
      <FlatList
        contentContainerStyle={styles.content}
        data={question.answerOptions}
        keyExtractor={(item) => item.id}
        ListHeaderComponent={(
          <View>
            <BackButton onPress={() => index > 0 ? setIndex((value) => value - 1) : router.back()} />
            <Text style={styles.code}>{assessment.code}</Text>
            <Text style={styles.progress}>Câu {index + 1} / {assessment.questions.length}</Text>
            <View style={styles.progressTrack}><View style={[styles.progressFill, { width: `${((index + 1) / assessment.questions.length) * 100}%` }]} /></View>
            <Text style={styles.question}>{question.questionText}</Text>
          </View>
        )}
        renderItem={({ item }) => {
          const checked = selected === item.id;
          return (
            <Pressable accessibilityRole="radio" accessibilityState={{ checked }} onPress={() => setAnswers((current) => ({ ...current, [question.id]: item.id }))} style={({ pressed }) => [styles.option, checked && styles.optionSelected, pressed && styles.optionPressed]}>
              <View style={[styles.radio, checked && styles.radioSelected]}>{checked ? <View style={styles.radioDot} /> : null}</View>
              <Text style={styles.optionText}>{item.optionText}</Text>
            </Pressable>
          );
        }}
        ListFooterComponent={(
          <View style={styles.footer}>
            {error ? <Text style={styles.error}>{error}</Text> : null}
            <ActionButton disabled={!selected || submitting} label={last ? 'Xem kết quả' : 'Câu tiếp theo'} loading={submitting} onPress={() => last ? void submit() : setIndex((value) => value + 1)} />
            <Text style={styles.disclaimer}>Kết quả chỉ có mục đích sàng lọc và không thay thế chẩn đoán y khoa.</Text>
          </View>
        )}
      />
    </AppScreen>
  );
}

function BackButton({ onPress }: { onPress(): void }) {
  return <Pressable accessibilityLabel="Quay lại" accessibilityRole="button" onPress={onPress} style={styles.back}><Ionicons color={colors.ink} name="arrow-back" size={22} /></Pressable>;
}

function riskLabel(level: AssessmentResult['riskLevel']) {
  return ({ NORMAL: 'Trong ngưỡng bình thường', MILD: 'Mức nhẹ', MODERATE: 'Mức vừa', SEVERE: 'Mức nghiêm trọng', EXTREME: 'Mức rất nghiêm trọng' } as const)[level];
}

const styles = StyleSheet.create({
  content: { gap: spacing.sm, padding: spacing.page, paddingBottom: 64 },
  back: { alignItems: 'center', height: 44, justifyContent: 'center', marginBottom: spacing.md, width: 44 },
  code: { color: colors.brandDark, fontFamily: fonts.bold, fontSize: type.label },
  progress: { color: colors.muted, fontFamily: fonts.medium, fontSize: type.caption, marginTop: spacing.xs },
  progressTrack: { backgroundColor: colors.line, borderRadius: radius.pill, height: 6, marginBottom: spacing.lg, marginTop: spacing.xs, overflow: 'hidden' },
  progressFill: { backgroundColor: colors.brand, borderRadius: radius.pill, height: '100%' },
  question: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.section, lineHeight: 30, marginBottom: spacing.md },
  option: { alignItems: 'center', backgroundColor: colors.surfaceMuted, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, flexDirection: 'row', gap: spacing.sm, minHeight: 60, padding: spacing.md, ...shadows.card },
  optionSelected: { backgroundColor: colors.mint, borderColor: colors.brand },
  optionPressed: { opacity: 0.84 },
  radio: { alignItems: 'center', borderColor: colors.lineStrong, borderRadius: radius.pill, borderWidth: 2, height: 22, justifyContent: 'center', width: 22 },
  radioSelected: { borderColor: colors.brandDark },
  radioDot: { backgroundColor: colors.brandDark, borderRadius: radius.pill, height: 10, width: 10 },
  optionText: { color: colors.ink, flex: 1, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 23 },
  footer: { gap: spacing.sm, marginTop: spacing.md },
  error: { color: colors.danger, fontFamily: fonts.medium, fontSize: type.label },
  disclaimer: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, textAlign: 'center' },
  resultContent: { gap: spacing.sm, padding: spacing.page, paddingBottom: 64 },
  resultCard: { alignItems: 'center', backgroundColor: colors.mint, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, padding: spacing.lg, ...shadows.card },
  resultIcon: { alignItems: 'center', backgroundColor: colors.surfaceMuted, borderRadius: radius.card, height: 56, justifyContent: 'center', width: 56 },
  resultEyebrow: { color: colors.brandDark, fontFamily: fonts.semibold, fontSize: type.label, marginTop: spacing.sm },
  score: { color: colors.ink, fontFamily: fonts.bold, fontSize: 32, marginTop: spacing.xs },
  risk: { color: colors.mintInk, fontFamily: fonts.semibold, fontSize: type.cardTitle, marginTop: spacing.xs },
  notice: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 21, marginTop: spacing.md, textAlign: 'center' },
  recommendationTitle: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.section, marginBottom: spacing.xs, marginTop: spacing.lg },
  recommendation: { alignItems: 'flex-start', backgroundColor: colors.surfaceMuted, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, flexDirection: 'row', gap: spacing.sm, padding: spacing.md },
  recommendationText: { color: colors.inkSoft, flex: 1, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 21 },
});
