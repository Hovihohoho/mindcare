import Ionicons from '@expo/vector-icons/Ionicons';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors, fonts, spacing, type } from '@/theme/tokens';

type Props = { title: string; description: string; onBack?: () => void };

export function ScreenHeader({ title, description, onBack }: Props) {
  return (
    <View style={styles.container}>
      <View style={styles.titleRow}>
        {onBack ? (
          <Pressable accessibilityLabel="Quay lại" accessibilityRole="button" hitSlop={8} onPress={onBack} style={({ pressed }) => [styles.back, pressed && styles.pressed]}>
            <Ionicons color={colors.brandDark} name="arrow-back" size={22} />
          </Pressable>
        ) : null}
        <Text accessibilityRole="header" style={styles.title}>{title}</Text>
      </View>
      <Text style={styles.description}>{description}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { gap: spacing.xs, paddingHorizontal: spacing.md, paddingTop: spacing.sm, paddingBottom: spacing.md },
  titleRow: { alignItems: 'center', flexDirection: 'row', gap: spacing.sm },
  back: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: 12, borderWidth: 1, height: 40, justifyContent: 'center', width: 40 },
  pressed: { opacity: 0.75 },
  title: { color: colors.ink, flex: 1, fontFamily: fonts.bold, fontSize: type.title, lineHeight: 36, letterSpacing: -0.6 },
  description: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 23 },
});
