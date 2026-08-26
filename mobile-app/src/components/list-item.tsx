/* Hallmark · component: list item · genre: modern-minimal · theme: MindCare */
import Ionicons from '@expo/vector-icons/Ionicons';
import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors, fonts, spacing, type } from '@/theme/tokens';

type Props = { title: string; description?: string; icon?: keyof typeof Ionicons.glyphMap; meta?: string; trailing?: ReactNode; onPress?: () => void; showDivider?: boolean };

export function ListItem({ title, description, icon, meta, trailing, onPress, showDivider = true }: Props) {
  return (
    <Pressable accessibilityRole={onPress ? 'button' : undefined} disabled={!onPress} onPress={onPress} style={({ pressed }) => [styles.row, showDivider && styles.divider, pressed && styles.pressed]}>
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
  row: { alignItems: 'center', flexDirection: 'row', gap: spacing.sm, minHeight: 64, paddingVertical: spacing.sm },
  divider: { borderBottomColor: colors.line, borderBottomWidth: StyleSheet.hairlineWidth },
  pressed: { backgroundColor: colors.surfacePressed, opacity: 0.82 },
  icon: { width: 24 },
  body: { flex: 1, minWidth: 0 },
  title: { color: colors.ink, fontFamily: fonts.medium, fontSize: type.body, lineHeight: 22 },
  description: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: 2 },
  meta: { color: colors.tertiary, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: spacing.xxs },
});
