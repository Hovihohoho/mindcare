import { StyleSheet, Text, View } from 'react-native';
import { colors, fonts, spacing, type } from '@/theme/tokens';

type Props = { title: string; description: string };

export function ScreenHeader({ title, description }: Props) {
  return (
    <View style={styles.container}>
      <Text accessibilityRole="header" style={styles.title}>{title}</Text>
      <Text style={styles.description}>{description}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { gap: spacing.xs, paddingHorizontal: spacing.md, paddingTop: spacing.sm, paddingBottom: spacing.md },
  title: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.title, lineHeight: 36, letterSpacing: -0.6 },
  description: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 23 },
});
