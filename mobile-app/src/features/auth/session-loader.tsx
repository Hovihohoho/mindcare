import { StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, radius, spacing } from '@/theme/tokens';

export function SessionLoader() {
  return (
    <SafeAreaView accessibilityLabel="Đang kiểm tra phiên đăng nhập" style={styles.safeArea}>
      <View style={styles.content}>
        <View style={styles.brandSkeleton} />
        <View style={styles.titleSkeleton} />
        <View style={styles.copySkeleton} />
        <View style={styles.fieldSkeleton} />
        <View style={styles.fieldSkeleton} />
        <View style={styles.buttonSkeleton} />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { backgroundColor: colors.canvas, flex: 1 },
  content: { alignSelf: 'center', justifyContent: 'center', flex: 1, maxWidth: 430, paddingHorizontal: spacing.lg, width: '100%' },
  brandSkeleton: { backgroundColor: colors.skeleton, borderRadius: radius.input, height: 40, marginBottom: spacing.xxl, width: 195 },
  titleSkeleton: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 32, marginBottom: spacing.sm, width: '72%' },
  copySkeleton: { backgroundColor: colors.skeleton, borderRadius: radius.sm, height: 18, marginBottom: spacing.xl, width: '92%' },
  fieldSkeleton: { backgroundColor: colors.skeleton, borderRadius: radius.input, height: 76, marginBottom: spacing.md, width: '100%' },
  buttonSkeleton: { backgroundColor: colors.skeleton, borderRadius: radius.input, height: 52, marginTop: spacing.xxs, width: '100%' },
});
