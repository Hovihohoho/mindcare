/* Hallmark · component: section header · genre: modern-minimal · theme: MindCare */
import Ionicons from '@expo/vector-icons/Ionicons';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

type Props = { title: string; description?: string; actionLabel?: string; onAction?: () => void };

export function SectionHeader({ title, description, actionLabel, onAction }: Props) {
  return (
    <View style={styles.container}>
      <View style={styles.row}>
        <Text accessibilityRole="header" style={styles.title}>{title}</Text>
        {actionLabel && onAction ? (
          <Pressable accessibilityRole="button" hitSlop={6} onPress={onAction} style={({ pressed }) => [styles.action, pressed && styles.pressed]}>
            <Text numberOfLines={1} style={styles.actionLabel}>{actionLabel}</Text>
            <Ionicons color={colors.brand} name="chevron-forward" size={15} />
          </Pressable>
        ) : null}
      </View>
      {description ? <Text style={styles.description}>{description}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { gap: spacing.xs },
  row: { alignItems: 'center', flexDirection: 'row', gap: spacing.sm, justifyContent: 'space-between' },
  title: { color: colors.ink, flex: 1, fontFamily: fonts.semibold, fontSize: type.section, letterSpacing: -0.2, lineHeight: 28 },
  description: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 20 },
  action: { alignItems: 'center', borderRadius: radius.sm, flexDirection: 'row', minHeight: 44, paddingLeft: spacing.xs },
  actionLabel: { color: colors.brand, fontFamily: fonts.medium, fontSize: type.caption },
  pressed: { opacity: 0.68 },
});
