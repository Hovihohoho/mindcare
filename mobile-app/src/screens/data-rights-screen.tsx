import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, Share, StyleSheet, Text, TextInput, View } from 'react-native';

import { AppScreen } from '@/components/app-screen';
import { ActionButton } from '@/components/buttons';
import { ScreenHeader } from '@/components/screen-header';
import { useAuth } from '@/features/auth/auth-context';
import { authService } from '@/services/auth/auth.service';
import { privacyService } from '@/services/privacy/privacy.service';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

export default function DataRightsScreen() {
  const { session, logout } = useAuth();
  const [password, setPassword] = useState('');
  const [action, setAction] = useState<'export' | 'delete' | null>(null);

  const exportData = async () => {
    if (!session?.accessToken || action) return;
    setAction('export');
    try {
      const [account, wellbeing, ai] = await Promise.all([
        authService.exportMyData(session.accessToken),
        privacyService.exportEmotionData(session.accessToken),
        privacyService.exportAiData(session.accessToken),
      ]);
      await Share.share({ message: JSON.stringify({ exportedAt: new Date().toISOString(), account, wellbeing, ai }, null, 2), title: 'Dữ liệu MindCare của tôi' });
    } catch (error) {
      Alert.alert('Không thể xuất dữ liệu', error instanceof Error ? error.message : 'Vui lòng thử lại.');
    } finally {
      setAction(null);
    }
  };

  const performDeletion = async () => {
    if (!session?.accessToken) return;
    setAction('delete');
    try {
      await authService.verifyPassword(session.accessToken, password);
      await privacyService.deleteEmotionData(session.accessToken);
      await privacyService.deleteAiData(session.accessToken);
      await authService.permanentlyDelete(session.accessToken, password);
      await logout().catch(() => undefined);
      router.replace('/(auth)/login');
    } catch (error) {
      Alert.alert('Chưa thể hoàn tất', error instanceof Error ? error.message : 'Bạn có thể thử lại an toàn.');
    } finally {
      setAction(null);
    }
  };

  const confirmDeletion = () => {
    if (!password) {
      Alert.alert('Cần mật khẩu', 'Nhập mật khẩu hiện tại để xác nhận.');
      return;
    }
    Alert.alert('Xóa vĩnh viễn tài khoản?', 'Nhật ký, dữ liệu sức khỏe, đánh giá và hội thoại AI sẽ không thể khôi phục.', [
      { text: 'Hủy', style: 'cancel' },
      { text: 'Xóa vĩnh viễn', style: 'destructive', onPress: () => void performDeletion() },
    ]);
  };

  return (
    <AppScreen>
      <ScreenHeader description="Tải bản sao hoặc xóa dữ liệu của bạn" title="Quyền dữ liệu" />
      <View style={styles.card}>
        <Text style={styles.title}>Tải dữ liệu của tôi</Text>
        <Text style={styles.description}>Bao gồm hồ sơ, nhật ký, đánh giá, Health Connect, kế hoạch tự chăm sóc và hội thoại AI.</Text>
        <ActionButton disabled={action !== null} label="Chia sẻ bản xuất JSON" loading={action === 'export'} onPress={() => void exportData()} tone="secondary" />
      </View>
      <View style={[styles.card, styles.dangerCard]}>
        <Text style={[styles.title, styles.dangerTitle]}>Xóa tài khoản vĩnh viễn</Text>
        <Text style={styles.description}>Nhập mật khẩu hiện tại. Thao tác này không thể hoàn tác.</Text>
        <TextInput accessibilityLabel="Mật khẩu hiện tại" autoCapitalize="none" onChangeText={setPassword} placeholder="Mật khẩu hiện tại" secureTextEntry style={styles.input} value={password} />
        <ActionButton disabled={action !== null} label="Xóa vĩnh viễn tài khoản" loading={action === 'delete'} onPress={confirmDeletion} tone="danger" />
      </View>
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  card: { backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.card, borderWidth: 1, gap: spacing.md, marginHorizontal: spacing.page, marginTop: spacing.md, padding: spacing.md },
  dangerCard: { borderColor: colors.dangerLine },
  title: { color: colors.ink, fontFamily: fonts.semibold, fontSize: type.cardTitle },
  dangerTitle: { color: colors.danger },
  description: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 20 },
  input: { backgroundColor: colors.surface, borderColor: colors.lineStrong, borderRadius: radius.input, borderWidth: 1, color: colors.ink, fontFamily: fonts.regular, minHeight: 48, paddingHorizontal: spacing.md },
});
