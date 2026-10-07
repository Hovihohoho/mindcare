import { Redirect, router } from 'expo-router';
import { useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text } from 'react-native';
import { ActionButton } from '@/components/buttons';
import { AppScreen } from '@/components/app-screen';
import { ScreenHeader } from '@/components/screen-header';
import { ApiError, AuthField, PasswordToggle, SuccessNotice } from '@/features/auth/auth-components';
import { useAuth } from '@/features/auth/auth-context';
import { AuthServiceError, authService } from '@/services/auth/auth.service';
import { colors, fonts, spacing, type } from '@/theme/tokens';

function passwordValidation(current: string, next: string, confirm: string) {
  if (!current.trim() || !next || !confirm) return 'Vui lòng nhập đủ mật khẩu hiện tại, mật khẩu mới và xác nhận.';
  if (next.length < 8 || next.length > 72 || !/\p{L}/u.test(next) || !/\p{Nd}/u.test(next)) {
    return 'Mật khẩu mới phải dài 8–72 ký tự và có ít nhất một chữ cái, một chữ số.';
  }
  if (next !== confirm) return 'Mật khẩu xác nhận chưa khớp.';
  if (next === current) return 'Mật khẩu mới phải khác mật khẩu hiện tại.';
  return '';
}

export default function ChangePasswordScreen() {
  const { session, status, logout } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [hidden, setHidden] = useState(true);
  const [validationError, setValidationError] = useState('');
  const [serverError, setServerError] = useState('');
  const [success, setSuccess] = useState('');
  const [saving, setSaving] = useState(false);

  if (status === 'unauthenticated') return <Redirect href="/(auth)/login" />;
  if (status === 'loading' || !session) return <AppScreen><ScreenHeader title="Đổi mật khẩu" onBack={() => router.back()} /></AppScreen>;

  const submit = async () => {
    const invalid = passwordValidation(currentPassword, newPassword, confirmPassword);
    setValidationError(invalid); setServerError(''); setSuccess('');
    if (invalid) return;
    setSaving(true);
    try {
      await authService.changePassword(session.accessToken, currentPassword, newPassword);
      setSuccess('Mật khẩu đã đổi. Các phiên đăng nhập hiện có đã bị thu hồi.');
      setCurrentPassword(''); setNewPassword(''); setConfirmPassword('');
      await logout().catch(() => undefined);
      router.replace('/(auth)/login');
      Alert.alert('Đổi mật khẩu thành công', 'Các phiên đã đăng xuất. Hãy đăng nhập lại bằng mật khẩu mới.');
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Không thể đổi mật khẩu.';
      if (cause instanceof AuthServiceError && cause.status === 400) setValidationError(message);
      else setServerError(message);
    } finally { setSaving(false); }
  };

  return <AppScreen>
    <ScreenHeader title="Đổi mật khẩu" onBack={() => router.back()} />
    <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
      <Text style={styles.intro}>Để bảo vệ tài khoản, nhập mật khẩu hiện tại và mật khẩu mới.</Text>
      <AuthField autoComplete="current-password" autoCorrect={false} icon="lock-closed-outline" label="Mật khẩu hiện tại" onChangeText={(value) => { setCurrentPassword(value); setValidationError(''); setServerError(''); }} secureTextEntry={hidden} textContentType="password" trailing={<PasswordToggle hidden={hidden} onPress={() => setHidden((value) => !value)} />} value={currentPassword} />
      <AuthField autoComplete="new-password" autoCorrect={false} icon="key-outline" label="Mật khẩu mới" onChangeText={(value) => { setNewPassword(value); setValidationError(''); setServerError(''); }} secureTextEntry={hidden} textContentType="newPassword" trailing={<PasswordToggle hidden={hidden} onPress={() => setHidden((value) => !value)} />} value={newPassword} />
      <AuthField autoComplete="new-password" autoCorrect={false} icon="checkmark-circle-outline" label="Xác nhận mật khẩu mới" onChangeText={(value) => { setConfirmPassword(value); setValidationError(''); setServerError(''); }} secureTextEntry={hidden} textContentType="newPassword" value={confirmPassword} />
      <Text style={styles.policy}>Dùng 8–72 ký tự, có ít nhất một chữ cái và một chữ số.</Text>
      {validationError ? <Text accessibilityRole="alert" style={styles.validation}>{validationError}</Text> : null}
      {serverError ? <ApiError message={serverError} /> : null}
      {success ? <SuccessNotice message={success} /> : null}
      <ActionButton disabled={saving} label="Đổi mật khẩu" loading={saving} onPress={() => void submit()} />
    </ScrollView>
  </AppScreen>;
}

const styles = StyleSheet.create({
  content: { gap: spacing.xs, padding: spacing.page, paddingBottom: spacing.xxl },
  intro: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 22, marginBottom: spacing.sm },
  policy: { color: colors.muted, fontFamily: fonts.regular, fontSize: type.caption, marginBottom: spacing.sm },
  validation: { color: colors.danger, fontFamily: fonts.medium, fontSize: type.label },
});
