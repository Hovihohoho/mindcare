import { Link, useRouter } from 'expo-router';
import { useRef, useState } from 'react';
import { Text, TextInput, View } from 'react-native';

import { ActionButton } from '@/components/buttons';
import { AuthServiceError } from '@/services/auth/auth.service';
import { ApiError, AuthField, AuthScreen, authStyles, BrandMark, PasswordToggle } from './auth-components';
import { useAuth } from './auth-context';

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function RegisterScreen() {
  const { register } = useAuth();
  const router = useRouter();
  const emailRef = useRef<TextInput>(null);
  const passwordRef = useRef<TextInput>(null);
  const confirmRef = useRef<TextInput>(null);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordHidden, setPasswordHidden] = useState(true);
  const [confirmHidden, setConfirmHidden] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [apiError, setApiError] = useState('');
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});

  const submit = async () => {
    const nextErrors = {
      name: name.trim().length < 2 ? 'Họ và tên cần ít nhất 2 ký tự.' : undefined,
      email: !email.trim() ? 'Vui lòng nhập email.' : !emailPattern.test(email.trim()) ? 'Email chưa đúng định dạng.' : undefined,
      password: !password
        ? 'Vui lòng nhập mật khẩu.'
        : password.length < 8
          ? 'Mật khẩu cần ít nhất 8 ký tự.'
          : password.length > 72
            ? 'Mật khẩu không được quá 72 ký tự.'
            : !/[A-Za-zÀ-ỹ]/.test(password) || !/\d/.test(password)
              ? 'Mật khẩu cần có ít nhất một chữ cái và một chữ số.'
              : undefined,
      confirmPassword: !confirmPassword ? 'Vui lòng nhập lại mật khẩu.' : confirmPassword !== password ? 'Mật khẩu nhập lại chưa khớp.' : undefined,
    };
    setErrors(nextErrors);
    setApiError('');
    if (Object.values(nextErrors).some(Boolean)) return;

    setSubmitting(true);
    try {
      const normalizedEmail = email.trim().toLowerCase();
      await register({ fullName: name.trim(), email: normalizedEmail, password });
      router.replace({ pathname: '/(auth)/verify-email', params: { email: normalizedEmail } });
    } catch (error) {
      setApiError(error instanceof AuthServiceError ? error.message : 'Không thể lưu phiên đăng nhập. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  const clearError = (field: string) => setErrors((current) => ({ ...current, [field]: undefined }));

  return (
    <AuthScreen>
      <BrandMark />
      <View style={authStyles.header}>
        <Text style={authStyles.title}>Tạo tài khoản</Text>
        <Text style={authStyles.description}>Bắt đầu không gian riêng để theo dõi cảm xúc và nhận hỗ trợ phù hợp.</Text>
      </View>
      <View style={authStyles.form}>
        <AuthField
          autoCapitalize="words"
          autoComplete="name"
          error={errors.name}
          icon="person-outline"
          label="Họ và tên"
          onChangeText={(value) => { setName(value); clearError('name'); }}
          onSubmitEditing={() => emailRef.current?.focus()}
          placeholder="Nguyễn An"
          returnKeyType="next"
          textContentType="name"
          value={name}
        />
        <AuthField
          ref={emailRef}
          autoCapitalize="none"
          autoComplete="email"
          error={errors.email}
          icon="mail-outline"
          keyboardType="email-address"
          label="Email"
          onChangeText={(value) => { setEmail(value); clearError('email'); }}
          onSubmitEditing={() => passwordRef.current?.focus()}
          placeholder="ban@example.com"
          returnKeyType="next"
          textContentType="emailAddress"
          value={email}
        />
        <AuthField
          ref={passwordRef}
          autoComplete="new-password"
          error={errors.password}
          icon="lock-closed-outline"
          label="Mật khẩu"
          onChangeText={(value) => { setPassword(value); clearError('password'); }}
          onSubmitEditing={() => confirmRef.current?.focus()}
          placeholder="Tối thiểu 8 ký tự"
          returnKeyType="next"
          secureTextEntry={passwordHidden}
          textContentType="newPassword"
          trailing={<PasswordToggle hidden={passwordHidden} onPress={() => setPasswordHidden((value) => !value)} />}
          value={password}
        />
        <AuthField
          ref={confirmRef}
          autoComplete="new-password"
          error={errors.confirmPassword}
          icon="shield-checkmark-outline"
          label="Nhập lại mật khẩu"
          onChangeText={(value) => { setConfirmPassword(value); clearError('confirmPassword'); }}
          onSubmitEditing={submit}
          placeholder="Nhập lại mật khẩu"
          returnKeyType="done"
          secureTextEntry={confirmHidden}
          textContentType="newPassword"
          trailing={<PasswordToggle hidden={confirmHidden} onPress={() => setConfirmHidden((value) => !value)} />}
          value={confirmPassword}
        />
        {apiError ? <ApiError message={apiError} /> : null}
        <ActionButton
          disabled={submitting}
          label={apiError ? 'Thử lại' : 'Tạo tài khoản'}
          loading={submitting}
          onPress={submit}
        />
      </View>
      <View style={authStyles.switchRow}>
        <Text style={authStyles.switchCopy}>Đã có tài khoản?</Text>
        <Link asChild href="/(auth)/login">
          <ActionButton compact label="Đăng nhập" tone="link" />
        </Link>
      </View>
    </AuthScreen>
  );
}
