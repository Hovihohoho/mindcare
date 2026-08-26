import { useLocalSearchParams, useRouter } from 'expo-router';
import { useState } from 'react';
import { Text, View } from 'react-native';

import { ActionButton } from '@/components/buttons';
import { AuthServiceError } from '@/services/auth/auth.service';
import { ApiError, AuthField, AuthScreen, authStyles, BrandMark, SuccessNotice } from './auth-components';
import { useAuth } from './auth-context';

export default function VerifyEmailScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ email?: string }>();
  const email = typeof params.email === 'string' ? params.email : '';
  const { resendVerification, verifyEmail } = useAuth();
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState<'verify' | 'resend' | null>(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const submit = async () => {
    setError('');
    setMessage('');
    if (!email) {
      setError('Không tìm thấy email. Vui lòng đăng ký lại.');
      return;
    }
    if (code.length !== 6) {
      setError('Mã xác thực phải gồm đúng 6 chữ số.');
      return;
    }
    setLoading('verify');
    try {
      await verifyEmail(email, code);
      router.replace({ pathname: '/(auth)/login', params: { email, verified: '1' } });
    } catch (caught) {
      setError(caught instanceof AuthServiceError ? caught.message : 'Không thể xác thực. Vui lòng thử lại.');
    } finally {
      setLoading(null);
    }
  };

  const resend = async () => {
    setError('');
    setMessage('');
    if (!email) {
      setError('Không tìm thấy email. Vui lòng đăng ký lại.');
      return;
    }
    setLoading('resend');
    try {
      await resendVerification(email);
      setMessage('Mã mới đã được gửi. Vui lòng kiểm tra cả thư rác.');
    } catch (caught) {
      setError(caught instanceof AuthServiceError ? caught.message : 'Không thể gửi lại mã. Vui lòng thử lại.');
    } finally {
      setLoading(null);
    }
  };

  return (
    <AuthScreen>
      <BrandMark />
      <View style={authStyles.header}>
        <Text style={authStyles.title}>Xác thực tài khoản</Text>
        <Text style={authStyles.description}>Nhập mã gồm 6 chữ số đã gửi đến {email || 'email của bạn'}.</Text>
      </View>
      <View style={authStyles.form}>
        <AuthField
          autoComplete="one-time-code"
          editable={!loading}
          icon="mail-open-outline"
          keyboardType="number-pad"
          label="Mã xác thực"
          maxLength={6}
          onChangeText={(value) => { setCode(value.replace(/\D/g, '').slice(0, 6)); setError(''); }}
          onSubmitEditing={submit}
          placeholder="000000"
          returnKeyType="done"
          textContentType="oneTimeCode"
          value={code}
        />
        {error ? <ApiError message={error} /> : null}
        {message ? <SuccessNotice message={message} /> : null}
        <ActionButton
          disabled={Boolean(loading) || code.length !== 6}
          label={error ? 'Thử xác thực lại' : 'Xác thực'}
          loading={loading === 'verify'}
          onPress={submit}
        />
        <ActionButton
          disabled={Boolean(loading)}
          label="Gửi lại mã"
          loading={loading === 'resend'}
          onPress={resend}
          tone="secondary"
        />
      </View>
    </AuthScreen>
  );
}
