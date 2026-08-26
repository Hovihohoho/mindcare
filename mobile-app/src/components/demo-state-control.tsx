import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { useMockState, type DataScenario } from '@/state/mock-state';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

const scenarios: { id: DataScenario; label: string }[] = [
  { id: 'ready', label: 'Có dữ liệu' },
  { id: 'loading', label: 'Loading' },
  { id: 'empty', label: 'Trống' },
  { id: 'error', label: 'Lỗi API' },
];

export function DemoStateControl() {
  const { scenario, setScenario } = useMockState();
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Kiểm thử trạng thái dữ liệu</Text>
      <Text style={styles.description}>Áp dụng cho Nhật ký, Đánh giá, AI và Chuyên gia.</Text>
      <FlatList
        horizontal
        showsHorizontalScrollIndicator={false}
        data={scenarios}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => {
          const selected = scenario === item.id;
          return (
            <Pressable accessibilityRole="radio" accessibilityState={{ checked: selected }} onPress={() => setScenario(item.id)} style={({ pressed }) => [styles.option, selected && styles.selected, pressed && styles.pressed]}>
              <Text numberOfLines={1} style={[styles.label, selected && styles.selectedLabel]}>{item.label}</Text>
            </Pressable>
          );
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { backgroundColor: colors.surfaceMuted, borderRadius: radius.card, gap: spacing.xs, marginBottom: spacing.sm, padding: spacing.md },
  title: { color: colors.brandDeep, fontFamily: fonts.semibold, fontSize: type.cardTitle },
  description: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.caption, lineHeight: 18 },
  list: { gap: spacing.xs, paddingTop: spacing.xxs },
  option: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.pill, borderWidth: 1, height: 44, justifyContent: 'center', paddingHorizontal: spacing.sm },
  selected: { backgroundColor: colors.brandDeep, borderColor: colors.brandDeep },
  pressed: { opacity: 0.8 },
  label: { color: colors.inkSoft, fontFamily: fonts.medium, fontSize: type.caption },
  selectedLabel: { color: colors.surface, fontFamily: fonts.semibold },
});
