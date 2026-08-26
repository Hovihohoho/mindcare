/* Hallmark · component: chat bubble · genre: modern-minimal · theme: MindCare */
import { StyleSheet, Text, View } from 'react-native';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

type Props = { role: 'assistant' | 'user'; text: string; time: string; sources?: string[] };

export function ChatBubble({ role, text, time, sources }: Props) {
  const mine = role === 'user';
  return (
    <View style={[styles.row, mine && styles.rowMine]}>
      <View style={[styles.content, mine && styles.mineBubble]}>
        <Text style={[styles.text, mine && styles.mineText]}>{text}</Text>
        {sources?.length ? <Text style={styles.sources}>Nguồn: {sources.join(' · ')}</Text> : null}
        <Text style={[styles.time, mine && styles.mineTime]}>{time}</Text>
      </View>
    </View>
  );
}

export function ChatBubbleLoading() {
  return (
    <View accessibilityLabel="AI đang trả lời" style={styles.row}>
      <View style={styles.loading}>
        <View style={styles.loadingLine} />
        <View style={styles.loadingShortLine} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { alignSelf: 'flex-start', maxWidth: '92%' },
  rowMine: { alignSelf: 'flex-end', maxWidth: '84%' },
  content: { paddingVertical: spacing.xs },
  mineBubble: { backgroundColor: colors.surfaceMuted, borderRadius: radius.card, paddingHorizontal: spacing.sm, paddingVertical: spacing.sm },
  text: { color: colors.ink, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 23 },
  mineText: { color: colors.ink },
  sources: { color: colors.brandDark, fontFamily: fonts.medium, fontSize: type.caption, lineHeight: 18, marginTop: spacing.xs },
  time: { color: colors.tertiary, fontFamily: fonts.regular, fontSize: type.caption, marginTop: spacing.xs },
  mineTime: { textAlign: 'right' },
  loading: { gap: spacing.xs, minHeight: 54, paddingVertical: spacing.sm, width: 160 },
  loadingLine: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 10, width: '92%' },
  loadingShortLine: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 10, width: '58%' },
});
