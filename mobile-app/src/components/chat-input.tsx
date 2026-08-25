/* Hallmark · component: chat input · genre: modern-minimal · theme: MindCare */
import Ionicons from '@expo/vector-icons/Ionicons';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

type Props = { value: string; disabled?: boolean; onChangeText(value: string): void; onSend(): void };

export function ChatInput({ value, disabled = false, onChangeText, onSend }: Props) {
  const sendDisabled = disabled || !value.trim();
  return (
    <View style={styles.composer}>
      <TextInput
        accessibilityLabel="Nhập tin nhắn cho AI"
        editable={!disabled}
        maxLength={4000}
        multiline
        onChangeText={onChangeText}
        placeholder="Chia sẻ điều bạn đang nghĩ…"
        placeholderTextColor={colors.tertiary}
        style={styles.input}
        value={value}
      />
      <Pressable accessibilityLabel="Gửi tin nhắn" accessibilityRole="button" disabled={sendDisabled} onPress={onSend} style={({ pressed }) => [styles.send, pressed && styles.pressed, sendDisabled && styles.disabled]}>
        <Ionicons color={colors.accentInk} name="arrow-up" size={20} />
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  composer: { alignItems: 'flex-end', backgroundColor: colors.surface, borderTopColor: colors.line, borderTopWidth: StyleSheet.hairlineWidth, flexDirection: 'row', gap: spacing.xs, paddingHorizontal: spacing.page, paddingVertical: spacing.sm },
  input: { backgroundColor: colors.canvas, borderColor: colors.line, borderRadius: radius.input, borderWidth: 1, color: colors.ink, flex: 1, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 22, maxHeight: 112, minHeight: 48, paddingHorizontal: spacing.sm, paddingVertical: spacing.sm },
  send: { alignItems: 'center', backgroundColor: colors.brand, borderRadius: radius.input, height: 48, justifyContent: 'center', width: 48 },
  pressed: { opacity: 0.82, transform: [{ translateY: 1 }] },
  disabled: { opacity: 0.4 },
});
