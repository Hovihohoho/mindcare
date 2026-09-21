import type { PropsWithChildren } from 'react';
import { KeyboardAvoidingView, Platform, StyleSheet, View, type ViewStyle } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '@/theme/tokens';

export function AppScreen({ children, backgroundColor = colors.canvas }: PropsWithChildren<{ backgroundColor?: ViewStyle['backgroundColor'] }>) {
  return (
    <SafeAreaView edges={['top', 'left', 'right']} style={[styles.safeArea, { backgroundColor }]}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        keyboardVerticalOffset={0}
        style={styles.keyboard}
      >
        <View style={[styles.frame, { backgroundColor }]}>
          <BackgroundWash />
          {children}
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function BackgroundWash() {
  return (
    <View pointerEvents="none" style={styles.wash}>
      <View style={[styles.washShape, styles.washBlue]} />
      <View style={[styles.washShape, styles.washMint]} />
      <View style={[styles.washShape, styles.washSand]} />
      <View style={[styles.washShape, styles.washBlush]} />
      <View style={[styles.washShape, styles.washLavender]} />
    </View>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: colors.canvas },
  keyboard: { flex: 1 },
  frame: { flex: 1, width: '100%', maxWidth: 768, alignSelf: 'center', backgroundColor: colors.canvas, overflow: 'hidden', position: 'relative' },
  wash: { bottom: 0, left: 0, overflow: 'hidden', position: 'absolute', right: 0, top: 0 },
  washShape: { opacity: 0.58, position: 'absolute' },
  washBlue: { backgroundColor: colors.washBlue, borderRadius: 220, height: 310, left: -124, top: -156, transform: [{ rotate: '-18deg' }], width: 510 },
  washMint: { backgroundColor: colors.washMint, borderRadius: 220, height: 350, left: -220, top: 178, transform: [{ rotate: '25deg' }], width: 420 },
  washSand: { backgroundColor: colors.washSand, borderRadius: 210, height: 320, right: -174, top: 14, transform: [{ rotate: '20deg' }], width: 400 },
  washBlush: { backgroundColor: colors.washBlush, borderRadius: 230, height: 390, right: -250, top: 285, transform: [{ rotate: '-28deg' }], width: 430 },
  washLavender: { backgroundColor: colors.washLavender, borderRadius: 220, bottom: -250, height: 420, left: -140, transform: [{ rotate: '18deg' }], width: 390 },
});
