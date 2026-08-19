import type { PropsWithChildren } from 'react';
import { KeyboardAvoidingView, Platform, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '@/theme/tokens';

export function AppScreen({ children }: PropsWithChildren) {
  return (
    <SafeAreaView edges={['top', 'left', 'right']} style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        keyboardVerticalOffset={0}
        style={styles.keyboard}
      >
        <View style={styles.frame}>{children}</View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: colors.canvas },
  keyboard: { flex: 1 },
  frame: { flex: 1, width: '100%', maxWidth: 430, alignSelf: 'center', backgroundColor: colors.canvas },
});
