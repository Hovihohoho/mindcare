import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, fonts, radius, spacing, type } from '@/theme/tokens';
import { emotionOptions } from './emotion.constants';
import { EmotionFaceIcon } from './emotion-face';
import type { EmotionLevel } from './emotion.types';

export function EmotionPicker({ value, onChange, disabled = false }: { value?: EmotionLevel; onChange(value: EmotionLevel): void; disabled?: boolean }) {
  return (
    <View accessibilityLabel="Chọn cảm xúc hôm nay" accessibilityRole="radiogroup" style={styles.row}>
      {emotionOptions.map((option) => {
        const selected = option.value === value;
        return (
          <Pressable
            accessibilityLabel={option.label}
            accessibilityRole="radio"
            accessibilityState={{ checked: selected, disabled }}
            disabled={disabled}
            key={option.value}
            onPress={() => onChange(option.value)}
            style={({ pressed }) => [styles.option, selected && { backgroundColor: option.surface, borderColor: option.color }, pressed && styles.pressed, disabled && styles.disabled]}
          >
            <EmotionFaceIcon color={option.color} face={option.face} size={36} />
            <Text numberOfLines={1} style={[styles.label, selected && styles.selectedLabel]}>{option.compactLabel}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: spacing.xxs, justifyContent: 'space-between' },
  option: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.input, borderWidth: 1, flex: 1, gap: spacing.xs, justifyContent: 'center', minHeight: 88, minWidth: 0, paddingHorizontal: spacing.xxs, paddingVertical: spacing.xs },
  pressed: { backgroundColor: colors.surfacePressed, transform: [{ translateY: 1 }] },
  disabled: { opacity: 0.5 },
  label: { color: colors.muted, fontFamily: fonts.medium, fontSize: type.tab, textAlign: 'center' },
  selectedLabel: { color: colors.ink, fontFamily: fonts.semibold },
});
