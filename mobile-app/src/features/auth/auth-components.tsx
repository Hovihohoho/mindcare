import Ionicons from '@expo/vector-icons/Ionicons';
import { forwardRef, type PropsWithChildren, type ReactNode, useState } from 'react';
import {
  KeyboardAvoidingView,
  Image,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  type TextInputProps,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

export function AuthScreen({ children }: PropsWithChildren) {
  return (
    <SafeAreaView edges={['top', 'bottom']} style={styles.safeArea}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.flex}>
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          <View style={styles.content}>{children}</View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

export function BrandMark() {
  return (
    <Image
      accessibilityLabel="MindCare"
      resizeMode="contain"
      source={require('../../../assets/images/mindcare-logo.png')}
      style={styles.brandLogo}
    />
  );
}

type AuthFieldProps = TextInputProps & {
  error?: string;
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  trailing?: ReactNode;
};

export const AuthField = forwardRef<TextInput, AuthFieldProps>(function AuthField(
  { error, icon, label, onBlur, onFocus, trailing, style, ...props },
  ref,
) {
  const [focused, setFocused] = useState(false);
  return (
    <View style={styles.fieldGroup}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <View style={[styles.inputShell, focused && styles.inputFocused, error && styles.inputError, props.editable === false && styles.inputDisabled]}>
        <Ionicons color={error ? colors.danger : colors.muted} name={icon} size={19} />
        <TextInput
          ref={ref}
          accessibilityLabel={label}
          onBlur={(event) => { setFocused(false); onBlur?.(event); }}
          onFocus={(event) => { setFocused(true); onFocus?.(event); }}
          placeholderTextColor={colors.muted}
          selectionColor={colors.brand}
          style={[styles.input, style]}
          {...props}
        />
        {trailing}
      </View>
      <Text accessibilityLiveRegion="polite" style={styles.errorText}>{error ?? ' '}</Text>
    </View>
  );
});

export function PasswordToggle({ hidden, onPress }: { hidden: boolean; onPress(): void }) {
  return (
    <Pressable
      accessibilityLabel={hidden ? 'Hiện mật khẩu' : 'Ẩn mật khẩu'}
      accessibilityRole="button"
      hitSlop={4}
      onPress={onPress}
      style={({ pressed }) => [styles.passwordToggle, pressed && styles.pressed]}
    >
      <Ionicons color={colors.muted} name={hidden ? 'eye-outline' : 'eye-off-outline'} size={20} />
    </Pressable>
  );
}

export function ApiError({ message }: { message: string }) {
  return (
    <View accessibilityLiveRegion="polite" style={styles.apiError}>
      <Ionicons color={colors.danger} name="alert-circle-outline" size={20} />
      <View style={styles.apiErrorBody}>
        <Text style={styles.apiErrorTitle}>Không thể kết nối</Text>
        <Text style={styles.apiErrorText}>{message}</Text>
      </View>
    </View>
  );
}

export function SuccessNotice({ message }: { message: string }) {
  return (
    <View accessibilityLiveRegion="polite" style={styles.successNotice}>
      <Ionicons color={colors.mintInk} name="checkmark-circle-outline" size={20} />
      <Text style={styles.successText}>{message}</Text>
    </View>
  );
}

export const authStyles = StyleSheet.create({
  header: { gap: spacing.xs, marginBottom: spacing.lg, marginTop: spacing.xl },
  title: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.title, letterSpacing: -0.5, lineHeight: 36 },
  description: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 23 },
  form: { gap: spacing.md },
  switchRow: { alignItems: 'center', flexDirection: 'row', justifyContent: 'center', marginTop: spacing.lg, minHeight: 44 },
  switchCopy: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.label },
  switchLink: { justifyContent: 'center', minHeight: 44, paddingHorizontal: spacing.xs },
  switchLinkText: { color: colors.brandDark, fontFamily: fonts.semibold, fontSize: type.label },
});

const styles = StyleSheet.create({
  flex: { flex: 1 },
  safeArea: { backgroundColor: colors.canvas, flex: 1 },
  scrollContent: { flexGrow: 1, justifyContent: 'center', paddingBottom: spacing.xl, paddingHorizontal: spacing.lg, paddingTop: spacing.lg },
  content: { alignSelf: 'center', maxWidth: 430, width: '100%' },
  brandLogo: { height: 40, width: 195 },
  fieldGroup: { gap: spacing.xs },
  fieldLabel: { color: colors.ink, fontFamily: fonts.medium, fontSize: type.label },
  inputShell: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.lineStrong, borderRadius: radius.input, borderWidth: 1, flexDirection: 'row', minHeight: 52, paddingLeft: spacing.sm },
  inputFocused: { borderColor: colors.brand, shadowColor: colors.brandSoft, shadowOpacity: 1, shadowRadius: 0, shadowOffset: { width: 0, height: 0 } },
  inputError: { borderColor: colors.danger },
  inputDisabled: { backgroundColor: colors.surfaceMuted, opacity: 0.6 },
  input: { color: colors.ink, flex: 1, fontFamily: fonts.regular, fontSize: type.body, minHeight: 50, paddingHorizontal: spacing.sm, paddingVertical: spacing.sm },
  passwordToggle: { alignItems: 'center', borderRadius: radius.sm, height: 44, justifyContent: 'center', marginRight: spacing.xxs, width: 44 },
  pressed: { backgroundColor: colors.surfacePressed, opacity: 0.84 },
  errorText: { color: colors.danger, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18, minHeight: 18 },
  apiError: { alignItems: 'flex-start', backgroundColor: colors.dangerSoft, borderColor: colors.dangerLine, borderRadius: radius.input, borderWidth: 1, flexDirection: 'row', gap: spacing.xs, padding: spacing.sm },
  apiErrorBody: { flex: 1, gap: spacing.xxs },
  apiErrorTitle: { color: colors.danger, fontFamily: fonts.semibold, fontSize: type.label },
  apiErrorText: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18 },
  successNotice: { alignItems: 'center', backgroundColor: colors.mint, borderRadius: radius.input, flexDirection: 'row', gap: spacing.xs, padding: spacing.sm },
  successText: { color: colors.mintInk, flex: 1, fontFamily: fonts.medium, fontSize: type.caption, lineHeight: 18 },
});
