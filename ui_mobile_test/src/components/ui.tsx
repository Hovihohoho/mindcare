import Ionicons from '@expo/vector-icons/Ionicons';
import { PropsWithChildren } from 'react';
import { Pressable, StyleSheet, Text, View, type StyleProp, type ViewStyle } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { palette, radius, space, type } from '../theme/tokens';

export function Screen({ children, style }: PropsWithChildren<{ style?: StyleProp<ViewStyle> }>) {
  return <SafeAreaView edges={['top']} style={[styles.screen, style]}><BackgroundWash />{children}</SafeAreaView>;
}

function BackgroundWash() {
  return <View pointerEvents="none" style={styles.wash}>
    <View style={[styles.washShape, styles.washBlue]} />
    <View style={[styles.washShape, styles.washMint]} />
    <View style={[styles.washShape, styles.washSand]} />
    <View style={[styles.washShape, styles.washBlush]} />
    <View style={[styles.washShape, styles.washLavender]} />
  </View>;
}

export function SectionTitle({ kicker, title, action }: { kicker?: string; title: string; action?: string }) {
  return <View style={styles.sectionTitle}><View style={styles.titleCopy}>{kicker ? <Text style={styles.kicker}>{kicker}</Text> : null}<Text style={styles.title}>{title}</Text></View>{action ? <Text style={styles.action}>{action}</Text> : null}</View>;
}

export function IconBubble({ icon, tone = 'mint' }: { icon: keyof typeof Ionicons.glyphMap; tone?: 'mint' | 'apricot' | 'lilac' | 'sky' | 'rose' }) {
  const fills = { mint: palette.mint, apricot: palette.apricot, lilac: palette.lilac, sky: palette.sky, rose: palette.rose };
  const inks = { mint: palette.mintDeep, apricot: palette.apricotDeep, lilac: palette.lilacDeep, sky: palette.skyDeep, rose: palette.roseDeep };
  return <View style={[styles.iconBubble, { backgroundColor: fills[tone] }]}><Ionicons color={inks[tone]} name={icon} size={20} /></View>;
}

export function SoftButton({ label, icon, onPress, filled = false }: { label: string; icon?: keyof typeof Ionicons.glyphMap; onPress?: () => void; filled?: boolean }) {
  return <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.button, filled && styles.buttonFilled, pressed && styles.pressed]}>{icon ? <Ionicons color={filled ? palette.primaryInk : palette.primary} name={icon} size={17} /> : null}<Text numberOfLines={1} style={[styles.buttonText, filled && styles.buttonTextFilled]}>{label}</Text></Pressable>;
}

export function BackButton({ onPress }: { onPress?: () => void }) {
  return <Pressable accessibilityLabel="Quay lại" accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.back, pressed && styles.pressed]}><Ionicons color={palette.ink} name="arrow-back" size={22} /></Pressable>;
}

const styles = StyleSheet.create({
  screen: { backgroundColor: palette.canvas, flex: 1, overflow: 'hidden', position: 'relative' },
  wash: { bottom: 0, left: 0, overflow: 'hidden', position: 'absolute', right: 0, top: 0 },
  washShape: { opacity: .62, position: 'absolute' },
  washBlue: { backgroundColor: palette.washBlue, borderRadius: 220, height: 310, left: -124, top: -156, transform: [{ rotate: '-18deg' }], width: 510 },
  washMint: { backgroundColor: palette.washMint, borderRadius: 220, height: 350, left: -220, top: 178, transform: [{ rotate: '25deg' }], width: 420 },
  washSand: { backgroundColor: palette.washSand, borderRadius: 210, height: 320, right: -174, top: 14, transform: [{ rotate: '20deg' }], width: 400 },
  washBlush: { backgroundColor: palette.washBlush, borderRadius: 230, height: 390, right: -250, top: 285, transform: [{ rotate: '-28deg' }], width: 430 },
  washLavender: { backgroundColor: palette.washLavender, borderRadius: 220, bottom: -250, height: 420, left: -140, transform: [{ rotate: '18deg' }], width: 390 },
  sectionTitle: { alignItems: 'flex-end', flexDirection: 'row', justifyContent: 'space-between', marginBottom: space.md },
  titleCopy: { flex: 1 }, kicker: { color: palette.primary, fontSize: type.overline, fontWeight: '800', letterSpacing: .8, marginBottom: 3 },
  title: { color: palette.ink, fontSize: type.title, fontWeight: '700', letterSpacing: -.25 }, action: { color: palette.primary, fontSize: type.label, fontWeight: '700', paddingBottom: 2 },
  iconBubble: { alignItems: 'center', borderRadius: radius.sm, height: 40, justifyContent: 'center', width: 40 },
  button: { alignItems: 'center', alignSelf: 'flex-start', borderColor: palette.primary, borderRadius: radius.sm, borderWidth: 1, flexDirection: 'row', gap: 7, minHeight: 44, paddingHorizontal: 14 },
  buttonFilled: { backgroundColor: palette.primary, borderColor: palette.primary }, buttonText: { color: palette.primary, fontSize: type.label, fontWeight: '800' }, buttonTextFilled: { color: palette.primaryInk },
  back: { alignItems: 'center', height: 44, justifyContent: 'center', width: 44 }, pressed: { opacity: .72, transform: [{ scale: .98 }] },
});
