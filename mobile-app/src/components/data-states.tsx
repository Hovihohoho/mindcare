import Ionicons from '@expo/vector-icons/Ionicons';
import { FlatList, StyleSheet, Text, View } from 'react-native';
import { ActionButton } from '@/components/buttons';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

export function SkeletonList({ rows = 4 }: { rows?: number }) {
  return (
    <FlatList
      accessibilityLabel="Đang tải nội dung"
      data={Array.from({ length: rows }, (_, index) => index)}
      keyExtractor={(item) => String(item)}
      contentContainerStyle={styles.skeletonList}
      renderItem={() => (
        <View style={styles.skeletonCard}>
          <View style={styles.skeletonIcon} />
          <View style={styles.skeletonBody}>
            <View style={styles.skeletonTitle} />
            <View style={styles.skeletonLine} />
            <View style={styles.skeletonShortLine} />
          </View>
        </View>
      )}
    />
  );
}

type FeedbackProps = {
  kind: 'empty' | 'error';
  title: string;
  description: string;
  onAction?: () => void;
  actionLabel?: string;
};

export function DataFeedback({ kind, title, description, onAction, actionLabel }: FeedbackProps) {
  return (
    <View accessibilityRole="summary" style={styles.feedback}>
      <View style={[styles.feedbackIcon, kind === 'error' && styles.errorIcon]}>
        <Ionicons color={kind === 'error' ? colors.danger : colors.brand} name={kind === 'error' ? 'cloud-offline-outline' : 'file-tray-outline'} size={26} />
      </View>
      <Text style={styles.feedbackTitle}>{title}</Text>
      <Text style={styles.feedbackDescription}>{description}</Text>
      {onAction && actionLabel ? <ActionButton icon={kind === 'error' ? 'refresh-outline' : 'options-outline'} label={actionLabel} onPress={onAction} tone="secondary" /> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  skeletonList: { paddingHorizontal: spacing.page, paddingBottom: 96 },
  skeletonCard: { borderBottomColor: colors.line, borderBottomWidth: StyleSheet.hairlineWidth, flexDirection: 'row', gap: spacing.sm, minHeight: 96, paddingVertical: spacing.md },
  skeletonIcon: { backgroundColor: colors.skeleton, borderRadius: radius.pill, height: 40, width: 40 },
  skeletonBody: { flex: 1, gap: spacing.sm, paddingTop: spacing.xxs },
  skeletonTitle: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 18, width: '62%' },
  skeletonLine: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 12, width: '100%' },
  skeletonShortLine: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 12, width: '76%' },
  feedback: { alignItems: 'center', backgroundColor: colors.surfaceMuted, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, justifyContent: 'center', margin: spacing.page, minHeight: 220, padding: spacing.lg },
  feedbackIcon: { alignItems: 'center', backgroundColor: colors.mint, borderRadius: radius.sm, height: 48, justifyContent: 'center', marginBottom: spacing.sm, width: 48 },
  errorIcon: { backgroundColor: colors.dangerSoft },
  feedbackTitle: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.section, lineHeight: 28, textAlign: 'center' },
  feedbackDescription: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 23, marginBottom: spacing.lg, marginTop: spacing.xs, textAlign: 'center' },
});
