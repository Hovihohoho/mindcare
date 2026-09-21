import Ionicons from '@expo/vector-icons/Ionicons';
import { useRouter } from 'expo-router';
import { PropsWithChildren } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { palette, radius, space, type } from '../theme/tokens';
import { BackButton, Screen } from './ui';

export function DetailScreen({ title, children }: PropsWithChildren<{ title: string }>) {
  const router = useRouter();
  return <Screen><View style={styles.header}><BackButton onPress={() => router.back()} /><Text numberOfLines={1} style={styles.headerTitle}>{title}</Text><View style={styles.headerEnd} /></View><ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>{children}</ScrollView></Screen>;
}

export function PageIntro({ eyebrow, title, text }: { eyebrow: string; title: string; text?: string }) {
  return <View style={styles.intro}><Text style={styles.eyebrow}>{eyebrow}</Text><Text style={styles.pageTitle}>{title}</Text>{text ? <Text style={styles.introText}>{text}</Text> : null}</View>;
}

export function Surface({ children, tone = 'surface' }: PropsWithChildren<{ tone?: 'surface' | 'mint' | 'apricot' | 'sky' }>) {
  const backgrounds = { surface: palette.lilac, mint: palette.mint, apricot: palette.apricot, sky: palette.sky };
  return <View style={[styles.surface, { backgroundColor: backgrounds[tone] }]}>{children}</View>;
}

export function Row({ icon, title, detail, onPress, last = false, accessory }: { icon: keyof typeof Ionicons.glyphMap; title: string; detail?: string; onPress?: () => void; last?: boolean; accessory?: string }) {
  const body = <><Ionicons color={palette.muted} name={icon} size={20} /><View style={styles.rowCopy}><Text style={styles.rowTitle}>{title}</Text>{detail ? <Text style={styles.rowDetail}>{detail}</Text> : null}</View>{accessory ? <Text style={styles.accessory}>{accessory}</Text> : null}{onPress ? <Ionicons color={palette.muted} name="chevron-forward" size={18} /> : null}</>;
  return onPress ? <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.row, !last && styles.rowRule, pressed && styles.pressed]}>{body}</Pressable> : <View style={[styles.row, !last && styles.rowRule]}>{body}</View>;
}

export function PrimaryAction({ label, icon, onPress }: { label: string; icon?: keyof typeof Ionicons.glyphMap; onPress?: () => void }) {
  return <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.primaryAction, pressed && styles.pressed]}>{icon ? <Ionicons color={palette.primaryInk} name={icon} size={18} /> : null}<Text style={styles.primaryText}>{label}</Text></Pressable>;
}

const styles = StyleSheet.create({
  header: { alignItems: 'center', flexDirection: 'row', minHeight: 60, paddingHorizontal: space.xs }, headerTitle: { color: palette.ink, flex: 1, fontSize: type.label, fontWeight: '700', textAlign: 'center' }, headerEnd: { width: 44 }, content: { padding: space.page, paddingBottom: 40 }, intro: { marginBottom: space.xxl }, eyebrow: { color: palette.primary, fontSize: type.overline, fontWeight: '800', letterSpacing: .8 }, pageTitle: { color: palette.ink, fontSize: type.display, fontWeight: '700', letterSpacing: -.8, marginTop: 4 }, introText: { color: palette.muted, fontSize: type.body, lineHeight: 23, marginTop: space.sm }, surface: { borderColor: palette.line, borderRadius: radius.md, borderWidth: 1, marginBottom: space.md, overflow: 'hidden' }, row: { alignItems: 'center', flexDirection: 'row', gap: space.sm, minHeight: 72, paddingHorizontal: space.md }, rowRule: { borderBottomColor: palette.line, borderBottomWidth: StyleSheet.hairlineWidth }, rowCopy: { flex: 1 }, rowTitle: { color: palette.ink, fontSize: type.label, fontWeight: '700' }, rowDetail: { color: palette.muted, fontSize: 12, lineHeight: 17, marginTop: 3 }, accessory: { color: palette.muted, fontSize: 12 }, primaryAction: { alignItems: 'center', backgroundColor: palette.primary, borderRadius: radius.sm, flexDirection: 'row', gap: space.xs, justifyContent: 'center', minHeight: 48, paddingHorizontal: space.md }, primaryText: { color: palette.primaryInk, fontSize: type.label, fontWeight: '800' }, pressed: { opacity: .76 },
});
