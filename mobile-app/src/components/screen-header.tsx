/* Hallmark · component: screen header · genre: modern-minimal · theme: MindCare */
import Ionicons from '@expo/vector-icons/Ionicons';
import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors, fonts, spacing, type } from '@/theme/tokens';

type Props = { title: string; description?: string; onBack?: () => void; action?: ReactNode };

export function ScreenHeader({ title, description, onBack, action }: Props) {
  return (
    <View style={styles.container}>
      <View style={styles.titleRow}>
        {onBack ? (
          <Pressable accessibilityLabel="Quay lại" accessibilityRole="button" hitSlop={8} onPress={onBack} style={({ pressed }) => [styles.back, pressed && styles.pressed]}>
            <Ionicons color={colors.brandDark} name="arrow-back" size={22} />
          </Pressable>
        ) : null}
        <Text accessibilityRole="header" style={styles.title}>{title}</Text>
        {action}
      </View>
      {description ? <Text style={styles.description}>{description}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { gap: spacing.xs, paddingHorizontal: spacing.page, paddingTop: spacing.sm, paddingBottom: spacing.md },
  titleRow: { alignItems: 'center', flexDirection: 'row', gap: spacing.sm },
  back: { alignItems: 'center', borderRadius: 12, height: 44, justifyContent: 'center', width: 44 },
  pressed: { opacity: 0.75 },
  title: { color: colors.ink, flex: 1, fontFamily: fonts.semibold, fontSize: type.title, lineHeight: 38, letterSpacing: -0.7 },
  description: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 20, maxWidth: 350 },
});
