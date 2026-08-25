import Ionicons from '@expo/vector-icons/Ionicons';
import { ActivityIndicator, Pressable, StyleSheet, Text, type PressableProps } from 'react-native';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

type ButtonProps = PressableProps & {
  label: string;
  icon?: keyof typeof Ionicons.glyphMap;
  tone?: 'primary' | 'secondary' | 'danger' | 'link';
  compact?: boolean;
  loading?: boolean;
};

export function ActionButton({ label, icon, tone = 'primary', compact = false, loading = false, style, ...props }: ButtonProps) {
  const foreground = tone === 'primary' ? colors.surface : tone === 'danger' ? colors.danger : colors.brandDark;
  return (
    <Pressable
      {...props}
      accessibilityRole="button"
      style={(state) => [
        styles.base,
        tone === 'primary' && styles.primary,
        tone === 'secondary' && styles.secondary,
        tone === 'danger' && styles.danger,
        tone === 'link' && styles.link,
        compact && styles.compact,
        state.pressed && styles.pressed,
        (props.disabled || loading) && styles.disabled,
        typeof style === 'function' ? style(state) : style,
      ]}
      accessibilityState={{ busy: loading, disabled: props.disabled || loading }}
      disabled={props.disabled || loading}
    >
      {loading ? <ActivityIndicator color={foreground} size="small" /> : icon ? <Ionicons color={foreground} name={icon} size={18} /> : null}
      <Text numberOfLines={1} style={[styles.label, tone === 'primary' ? styles.primaryLabel : tone === 'danger' ? styles.dangerLabel : styles.secondaryLabel]}>{loading ? 'Đang xử lý…' : label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: { alignItems: 'center', borderRadius: radius.input, flexDirection: 'row', gap: spacing.xs, height: 52, justifyContent: 'center', paddingHorizontal: spacing.md },
  primary: { backgroundColor: colors.brandDark },
  secondary: { backgroundColor: colors.surface, borderColor: colors.line, borderWidth: 1 },
  danger: { backgroundColor: colors.dangerSoft, borderColor: colors.dangerLine, borderWidth: 1 },
  link: { backgroundColor: colors.transparent },
  compact: { height: 44, paddingHorizontal: spacing.xs },
  pressed: { opacity: 0.82, transform: [{ translateY: 1 }] },
  disabled: { opacity: 0.5 },
  label: { fontFamily: fonts.semibold, fontSize: type.label },
  primaryLabel: { color: colors.accentInk },
  secondaryLabel: { color: colors.brandDark },
  dangerLabel: { color: colors.danger },
});
