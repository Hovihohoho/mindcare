/* Hallmark · component: screen header · genre: modern-minimal · theme: MindCare */
import Ionicons from '@expo/vector-icons/Ionicons';
import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

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
        <Text accessibilityRole="header" numberOfLines={1} style={[styles.title, onBack && styles.detailTitle]}>{title}</Text>
        {action ?? (onBack ? <View style={styles.headerEnd} /> : null)}
      </View>
      {description ? <Text style={styles.description}>{description}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  // Detail headers in the reference UI sit directly on the page canvas.  The
  // divider belongs to the content that follows, rather than becoming a
  // coloured app bar on every secondary route.
  container: { gap: spacing.xxs, minHeight: 60, paddingHorizontal: spacing.xs, paddingTop: spacing.xs, paddingBottom: spacing.xs },
  titleRow: { alignItems: 'center', flexDirection: 'row', gap: spacing.sm },
  back: { alignItems: 'center', borderRadius: radius.sm, height: 44, justifyContent: 'center', width: 44 },
  pressed: { opacity: 0.75 },
  title: { color: colors.ink, flex: 1, fontFamily: fonts.semibold, fontSize: type.section, lineHeight: 28, letterSpacing: -0.25 },
  detailTitle: { fontSize: type.label, lineHeight: 20, textAlign: 'center' },
  headerEnd: { height: 44, width: 44 },
  description: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.label, lineHeight: 20, marginHorizontal: spacing.sm, maxWidth: 350 },
});
