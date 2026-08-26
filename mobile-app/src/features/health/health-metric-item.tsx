/* Hallmark · component: health metric item · genre: modern-minimal · theme: MindCare
 * states: default · selected · loading · empty · accessibility
 */
import Ionicons from '@expo/vector-icons/Ionicons';
import { StyleSheet, Text, View } from 'react-native';
import { colors, fonts, spacing, type } from '@/theme/tokens';

type Props = {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  value: string;
  note?: string;
  index: number;
};

export function HealthMetricItem({ icon, label, value, note, index }: Props) {
  return (
    <View
      accessibilityLabel={`${label}: ${value}${note ? `, ${note}` : ''}`}
      style={[styles.item, index % 2 === 0 && styles.rightRule, index < 2 && styles.bottomRule]}
    >
      <Ionicons color={colors.brand} name={icon} size={21} />
      <Text numberOfLines={1} style={styles.value}>{value}</Text>
      <Text style={styles.label}>{label}</Text>
      {note ? <Text numberOfLines={1} style={styles.note}>{note}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  item: { minHeight: 124, padding: spacing.md, width: '50%' },
  rightRule: { borderRightColor: colors.line, borderRightWidth: StyleSheet.hairlineWidth },
  bottomRule: { borderBottomColor: colors.line, borderBottomWidth: StyleSheet.hairlineWidth },
  value: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.section, letterSpacing: -0.3, lineHeight: 28, marginTop: spacing.sm },
  label: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: 2 },
  note: { color: colors.tertiary, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, marginTop: spacing.xxs },
});
