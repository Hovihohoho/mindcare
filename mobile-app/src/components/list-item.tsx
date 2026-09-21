/* Hallmark · component: list item · genre: modern-minimal · theme: MindCare */
import Ionicons from '@expo/vector-icons/Ionicons';
import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

type Props = { title: string; description?: string; icon?: keyof typeof Ionicons.glyphMap; meta?: string; trailing?: ReactNode; onPress?: () => void; showDivider?: boolean; groupStart?: boolean; groupEnd?: boolean };

export function ListItem({ title, description, icon, meta, trailing, onPress, showDivider = true, groupStart = false, groupEnd = false }: Props) {
  return (
    <Pressable accessibilityRole={onPress ? 'button' : undefined} disabled={!onPress} onPress={onPress} style={({ pressed }) => [styles.row, groupStart && styles.groupStart, groupEnd && styles.groupEnd, showDivider && styles.divider, pressed && styles.pressed]}>
      {icon ? <Ionicons color={colors.brandDark} name={icon} size={21} style={styles.icon} /> : null}
      <View style={styles.body}>
        <Text style={styles.title}>{title}</Text>
        {description ? <Text numberOfLines={2} style={styles.description}>{description}</Text> : null}
        {meta ? <Text style={styles.meta}>{meta}</Text> : null}
      </View>
      {trailing ?? (onPress ? <Ionicons color={colors.tertiary} name="chevron-forward" size={17} /> : null)}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  // `surfaceMuted` is the quiet lilac list surface used by the UI reference;
  // mint remains reserved for selected and successful states.
  row: { alignItems: 'center', backgroundColor: colors.surfaceMuted, borderLeftColor: colors.line, borderLeftWidth: StyleSheet.hairlineWidth, borderRightColor: colors.line, borderRightWidth: StyleSheet.hairlineWidth, flexDirection: 'row', gap: spacing.sm, minHeight: 72, paddingHorizontal: spacing.md, paddingVertical: spacing.sm },
  groupStart: { borderTopColor: colors.line, borderTopLeftRadius: radius.card, borderTopRightRadius: radius.card, borderTopWidth: StyleSheet.hairlineWidth },
  groupEnd: { borderBottomColor: colors.line, borderBottomLeftRadius: radius.card, borderBottomRightRadius: radius.card, borderBottomWidth: StyleSheet.hairlineWidth },
  divider: { borderBottomColor: colors.line, borderBottomWidth: StyleSheet.hairlineWidth },
  pressed: { backgroundColor: colors.surfacePressed, opacity: 0.82 },
  icon: { color: colors.muted, width: 24 },
  body: { flex: 1, minWidth: 0 },
  title: { color: colors.ink, fontFamily: fonts.medium, fontSize: type.body, lineHeight: 22 },
  description: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: 2 },
  meta: { color: colors.tertiary, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: spacing.xxs },
});
