import { Redirect, router } from 'expo-router';
import * as ImagePicker from 'expo-image-picker';
import { useEffect, useState } from 'react';
import { Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ActionButton } from '@/components/buttons';
import { AppScreen } from '@/components/app-screen';
import { ScreenHeader } from '@/components/screen-header';
import { SkeletonList } from '@/components/data-states';
import { ApiError, AuthField, SuccessNotice } from '@/features/auth/auth-components';
import { useAuth } from '@/features/auth/auth-context';
import type { AuthUser, UpdateProfileInput } from '@/features/auth/auth.types';
import { AuthServiceError, authService } from '@/services/auth/auth.service';
import { API_URL } from '@/services/api/api.config';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

type Gender = 'MALE' | 'FEMALE' | 'OTHER' | '';
type ProfileForm = { fullName: string; phone: string; birthDate: string; gender: Gender; address: string; bio: string };
type ProfileErrors = Partial<Record<keyof ProfileForm, string>>;

function formFromUser(user: AuthUser): ProfileForm {
  return { fullName: user.fullName, phone: user.phone ?? '', birthDate: user.birthDate ?? '', gender: user.gender ?? '', address: user.address ?? '', bio: user.bio ?? '' };
}

function validate(form: ProfileForm): ProfileErrors {
  const errors: ProfileErrors = {};
  if (!form.fullName.trim()) errors.fullName = 'Vui lòng nhập họ tên.';
  else if (form.fullName.trim().length > 100) errors.fullName = 'Họ tên tối đa 100 ký tự.';
  if (form.phone.length > 30) errors.phone = 'Số điện thoại tối đa 30 ký tự.';
  if (form.address.length > 500) errors.address = 'Địa chỉ tối đa 500 ký tự.';
  if (form.bio.length > 5000) errors.bio = 'Giới thiệu tối đa 5000 ký tự.';
  if (form.birthDate) {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(form.birthDate);
    if (!match) errors.birthDate = 'Ngày sinh dùng định dạng YYYY-MM-DD.';
    else {
      const [, year, month, day] = match;
      const parsed = new Date(Number(year), Number(month) - 1, Number(day));
      if (parsed.getFullYear() !== Number(year) || parsed.getMonth() !== Number(month) - 1 || parsed.getDate() !== Number(day)) {
        errors.birthDate = 'Ngày sinh không hợp lệ.';
      } else {
        const now = new Date();
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        if (parsed >= today) errors.birthDate = 'Ngày sinh phải ở trước hôm nay.';
      }
    }
  }
  return errors;
}

export default function ProfileScreen() {
  const { session, status, updateProfile } = useAuth();
  const [form, setForm] = useState<ProfileForm>({ fullName: '', phone: '', birthDate: '', gender: '', address: '', bio: '' });
  const [errors, setErrors] = useState<ProfileErrors>({});
  const [validationError, setValidationError] = useState('');
  const [serverError, setServerError] = useState('');
  const [success, setSuccess] = useState('');
  const [saving, setSaving] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [avatarError, setAvatarError] = useState('');

  useEffect(() => {
    if (session?.user) setForm(formFromUser(session.user));
  }, [session?.user]);

  if (status === 'unauthenticated') return <Redirect href="/(auth)/login" />;
  if (status === 'loading' || !session) return <AppScreen><ScreenHeader title="Thông tin cá nhân" onBack={() => router.back()} /><SkeletonList rows={3} /></AppScreen>;

  const change = (key: keyof ProfileForm, value: string) => {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: undefined }));
    setValidationError(''); setServerError(''); setSuccess('');
  };

  const submit = async () => {
    const nextErrors = validate(form);
    setErrors(nextErrors); setValidationError(''); setServerError(''); setSuccess('');
    if (Object.keys(nextErrors).length) { setValidationError('Vui lòng kiểm tra lại thông tin đã nhập.'); return; }
    const token = session.accessToken;
    const input: UpdateProfileInput = {
      fullName: form.fullName.trim(), phone: form.phone.trim() || null,
      birthDate: form.birthDate || null, gender: form.gender || null,
      address: form.address.trim() || null, bio: form.bio.trim() || null,
    };
    setSaving(true);
    try {
      const updated = await authService.updateProfile(token, input);
      await updateProfile(updated);
      setForm(formFromUser(updated));
      setSuccess('Thông tin cá nhân đã được cập nhật.');
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Không thể cập nhật hồ sơ.';
      if (cause instanceof AuthServiceError && cause.status === 400) setValidationError(message);
      else setServerError(message);
    } finally { setSaving(false); }
  };

  const chooseAvatar = async () => {
    setAvatarError(''); setSuccess('');
    try {
      const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], allowsEditing: true, aspect: [1, 1], quality: 0.9 });
      if (result.canceled) return;
      const asset = result.assets[0];
      const extension = asset.fileName?.split('.').pop()?.toLowerCase();
      const mimeType = asset.mimeType ?? (extension === 'png' ? 'image/png' : extension === 'webp' ? 'image/webp' : ['jpg', 'jpeg'].includes(extension ?? '') ? 'image/jpeg' : '');
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(mimeType)) {
        setAvatarError('Ảnh cần ở định dạng JPEG, PNG hoặc WebP.'); return;
      }
      if (asset.fileSize !== undefined && asset.fileSize > 5 * 1024 * 1024) {
        setAvatarError('Ảnh không được vượt quá 5MB.'); return;
      }
      setUploadingAvatar(true);
      const updated = await authService.uploadAvatar(session.accessToken, {
        uri: asset.uri, mimeType, fileName: asset.fileName ?? `avatar.${mimeType === 'image/png' ? 'png' : mimeType === 'image/webp' ? 'webp' : 'jpg'}`,
      });
      await updateProfile(updated);
      setSuccess('Ảnh đại diện đã được cập nhật.');
    } catch (cause) {
      setAvatarError(cause instanceof Error ? cause.message : 'Không thể cập nhật ảnh đại diện.');
    } finally { setUploadingAvatar(false); }
  };

  const avatarUri = session.user.avatarUrl
    ? session.user.avatarUrl.startsWith('http') ? session.user.avatarUrl : `${API_URL}${session.user.avatarUrl}`
    : null;

  return <AppScreen>
    <ScreenHeader title="Thông tin cá nhân" onBack={() => router.back()} />
    <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
      <Text style={styles.intro}>Cập nhật những thông tin hồ sơ MindCare hỗ trợ.</Text>
      <View style={styles.avatarBlock}>
        {avatarUri ? <Image source={{ uri: avatarUri }} style={styles.avatar} /> : <View style={styles.avatarPlaceholder}><Text style={styles.avatarInitial}>{session.user.fullName.trim().charAt(0).toLocaleUpperCase('vi-VN') || '?'}</Text></View>}
        <View style={styles.avatarActions}>
          <ActionButton compact disabled={uploadingAvatar || saving} label="Đổi ảnh đại diện" loading={uploadingAvatar} onPress={() => void chooseAvatar()} tone="secondary" />
          {avatarError ? <Text accessibilityRole="alert" style={styles.validation}>{avatarError}</Text> : null}
        </View>
      </View>
      <AuthField editable={false} icon="mail-outline" label="Email" value={session.user.email} />
      <AuthField autoCapitalize="words" autoCorrect={false} error={errors.fullName} icon="person-outline" label="Họ và tên" maxLength={100} onChangeText={(value) => change('fullName', value)} value={form.fullName} />
      <AuthField autoComplete="tel" error={errors.phone} icon="call-outline" keyboardType="phone-pad" label="Số điện thoại" maxLength={30} onChangeText={(value) => change('phone', value)} value={form.phone} />
      <AuthField autoCapitalize="none" error={errors.birthDate} icon="calendar-outline" label="Ngày sinh" maxLength={10} onChangeText={(value) => change('birthDate', value)} placeholder="YYYY-MM-DD" value={form.birthDate} />
      <View style={styles.genderGroup}>
        <Text style={styles.label}>Giới tính</Text>
        <View style={styles.choices}>
          {([['', 'Không cung cấp'], ['FEMALE', 'Nữ'], ['MALE', 'Nam'], ['OTHER', 'Khác']] as const).map(([value, label]) => (
            <Pressable key={value || 'none'} accessibilityRole="radio" accessibilityState={{ checked: form.gender === value }} onPress={() => change('gender', value)} style={[styles.choice, form.gender === value && styles.choiceSelected]}>
              <Text style={[styles.choiceText, form.gender === value && styles.choiceTextSelected]}>{label}</Text>
            </Pressable>
          ))}
        </View>
      </View>
      <AuthField autoCapitalize="sentences" error={errors.address} icon="location-outline" label="Địa chỉ" maxLength={500} onChangeText={(value) => change('address', value)} value={form.address} />
      <AuthField error={errors.bio} icon="document-text-outline" label="Giới thiệu" maxLength={5000} multiline onChangeText={(value) => change('bio', value)} style={styles.bioInput} textAlignVertical="top" value={form.bio} />
      {validationError ? <Text accessibilityRole="alert" style={styles.validation}>{validationError}</Text> : null}
      {serverError ? <ApiError message={serverError} /> : null}
      {success ? <SuccessNotice message={success} /> : null}
      <ActionButton disabled={saving} label="Lưu hồ sơ" loading={saving} onPress={() => void submit()} />
    </ScrollView>
  </AppScreen>;
}

const styles = StyleSheet.create({
  content: { gap: spacing.xs, padding: spacing.page, paddingBottom: spacing.xxl },
  intro: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.body, lineHeight: 22, marginBottom: spacing.sm },
  genderGroup: { gap: spacing.xs, marginBottom: spacing.sm },
  avatarBlock: { alignItems: 'center', flexDirection: 'row', gap: spacing.md, marginBottom: spacing.md },
  avatar: { backgroundColor: colors.surfaceMuted, borderRadius: radius.pill, height: 76, width: 76 },
  avatarPlaceholder: { alignItems: 'center', backgroundColor: colors.brandSoft, borderRadius: radius.pill, height: 76, justifyContent: 'center', width: 76 },
  avatarInitial: { color: colors.brandDeep, fontFamily: fonts.bold, fontSize: type.section },
  avatarActions: { flex: 1, gap: spacing.xs },
  label: { color: colors.inkSoft, fontFamily: fonts.medium, fontSize: type.label },
  choices: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.xs },
  choice: { backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.pill, borderWidth: 1, paddingHorizontal: spacing.md, paddingVertical: spacing.sm },
  choiceSelected: { backgroundColor: colors.brandSoft, borderColor: colors.brand },
  choiceText: { color: colors.inkSoft, fontFamily: fonts.medium, fontSize: type.label },
  choiceTextSelected: { color: colors.brandDeep },
  bioInput: { minHeight: 110, paddingTop: spacing.sm },
  validation: { color: colors.danger, fontFamily: fonts.medium, fontSize: type.label },
});
