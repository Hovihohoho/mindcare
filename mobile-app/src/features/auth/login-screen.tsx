import { Link, useLocalSearchParams } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Text, TextInput, View } from 'react-native';

import { ActionButton } from '@/components/buttons';
import { AuthServiceError } from '@/services/auth/auth.service';
import { ApiError, AuthField, AuthScreen, authStyles, BrandMark, PasswordToggle, SuccessNotice } from './auth-components';
import { useAuth } from './auth-context';

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function LoginScreen() {
  const { login } = useAuth();
  const params = useLocalSearchParams<{ email?: string; verified?: string }>();
  const passwordRef = useRef<TextInput>(null);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordHidden, setPasswordHidden] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [apiError, setApiError] = useState('');
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});

  useEffect(() => {
    if (typeof params.email === 'string') setEmail(params.email);
  }, [params.email]);

  const submit = async () => {
    const nextErrors = {
      email: !email.trim() ? 'Vui lòng nhập email.' : !emailPattern.test(email.trim()) ? 'Email chưa đúng định dạng.' : undefined,
      password: !password ? 'Vui lòng nhập mật khẩu.' : password.length < 8 ? 'Mật khẩu cần ít nhất 8 ký tự.' : undefined,
    };
    setErrors(nextErrors);
    setApiError('');
    if (nextErrors.email || nextErrors.password) return;

    setSubmitting(true);
    try {
      await login({ email: email.trim(), password });
    } catch (error) {
      setApiError(error instanceof AuthServiceError ? error.message : 'Không thể lưu phiên đăng nhập. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthScreen>
      <BrandMark />
      <View style={authStyles.header}>
        <Text style={authStyles.title}>Chào bạn trở lại</Text>
        <Text style={authStyles.description}>Đăng nhập để tiếp tục ghi nhận cảm xúc và chăm sóc sức khỏe tinh thần.</Text>
      </View>
      <View style={authStyles.form}>
        {params.verified === '1' ? <SuccessNotice message="Email đã được xác thực. Bạn có thể đăng nhập ngay." /> : null}
        <AuthField
          autoCapitalize="none"
          autoComplete="email"
          error={errors.email}
          icon="mail-outline"
          keyboardType="email-address"
          label="Email"
          onChangeText={(value) => { setEmail(value); setErrors((current) => ({ ...current, email: undefined })); }}
          onSubmitEditing={() => passwordRef.current?.focus()}
          placeholder="ban@example.com"
          returnKeyType="next"
          textContentType="emailAddress"
          value={email}
        />
        <AuthField
          ref={passwordRef}
          autoComplete="current-password"
          error={errors.password}
          icon="lock-closed-outline"
          label="Mật khẩu"
          onChangeText={(value) => { setPassword(value); setErrors((current) => ({ ...current, password: undefined })); }}
          onSubmitEditing={submit}
          placeholder="Tối thiểu 8 ký tự"
          returnKeyType="done"
          secureTextEntry={passwordHidden}
          textContentType="password"
          trailing={<PasswordToggle hidden={passwordHidden} onPress={() => setPasswordHidden((value) => !value)} />}
          value={password}
        />
        {apiError ? <ApiError message={apiError} /> : null}
        <ActionButton
          disabled={submitting}
          label={apiError ? 'Thử lại' : 'Đăng nhập'}
          loading={submitting}
          onPress={submit}
        />
      </View>
      <View style={authStyles.switchRow}>
        <Text style={authStyles.switchCopy}>Chưa có tài khoản?</Text>
        <Link asChild href="/(auth)/register">
          <ActionButton compact label="Đăng ký" tone="link" />
        </Link>
      </View>
    </AuthScreen>
  );
}
