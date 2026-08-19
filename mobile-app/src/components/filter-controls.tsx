import Ionicons from '@expo/vector-icons/Ionicons';
import { Modal, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { ActionButton } from '@/components/buttons';
import { colors, fonts, radius, shadows, spacing, type } from '@/theme/tokens';

export type FilterOption = { id: string; label: string };

type FilterButtonProps = { activeLabel?: string; onPress: () => void };

export function FilterButton({ activeLabel, onPress }: FilterButtonProps) {
  return (
    <View style={styles.filterRow}>
      <Pressable accessibilityRole="button" onPress={onPress} style={({ pressed }) => [styles.filterButton, pressed && styles.pressed]}>
        <Ionicons color={colors.brandDark} name="options-outline" size={18} />
        <Text numberOfLines={1} style={styles.filterButtonText}>Bộ lọc</Text>
      </Pressable>
      {activeLabel && (
        <View accessibilityLabel={`Đang áp dụng bộ lọc: ${activeLabel}`} style={styles.activeChip}>
          <View style={styles.dot} />
          <Text numberOfLines={1} style={styles.activeText}>{activeLabel}</Text>
        </View>
      )}
    </View>
  );
}

type SheetProps = {
  visible: boolean;
  title: string;
  options: FilterOption[];
  selected: string;
  onSelect: (value: string) => void;
  onClose: () => void;
};

export function FilterBottomSheet({ visible, title, options, selected, onSelect, onClose }: SheetProps) {
  const insets = useSafeAreaInsets();
  return (
    <Modal animationType="fade" onRequestClose={onClose} presentationStyle="overFullScreen" transparent visible={visible}>
      <View style={styles.modalRoot}>
        <Pressable accessibilityLabel="Đóng bộ lọc" onPress={onClose} style={styles.backdrop} />
        <View style={[styles.sheet, { paddingBottom: Math.max(insets.bottom, spacing.md) }]}>
          <View style={styles.handle} />
          <View style={styles.sheetHeader}>
            <Text accessibilityRole="header" style={styles.sheetTitle}>{title}</Text>
            <Pressable accessibilityLabel="Đóng" hitSlop={8} onPress={onClose} style={styles.closeButton}>
              <Ionicons color={colors.inkSoft} name="close" size={22} />
            </Pressable>
          </View>
          <FlatList
            data={options}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => {
              const checked = selected === item.id;
              return (
                <Pressable accessibilityRole="radio" accessibilityState={{ checked }} onPress={() => onSelect(item.id)} style={({ pressed }) => [styles.option, pressed && styles.optionPressed]}>
                  <Text style={[styles.optionText, checked && styles.optionTextActive]}>{item.label}</Text>
                  <Ionicons color={checked ? colors.brand : colors.lineStrong} name={checked ? 'radio-button-on' : 'radio-button-off'} size={22} />
                </Pressable>
              );
            }}
          />
          <View style={styles.sheetActions}>
            <ActionButton label="Xóa lọc" onPress={() => onSelect('all')} style={styles.flexButton} tone="secondary" />
            <ActionButton label="Áp dụng" onPress={onClose} style={styles.flexButton} />
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  filterRow: { alignItems: 'center', flexDirection: 'row', gap: spacing.xs, minHeight: 44 },
  filterButton: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.input, borderWidth: 1, flexDirection: 'row', gap: spacing.xs, height: 44, paddingHorizontal: spacing.sm },
  filterButtonText: { color: colors.brandDark, fontFamily: fonts.semibold, fontSize: type.label },
  pressed: { backgroundColor: colors.surfacePressed },
  activeChip: { alignItems: 'center', backgroundColor: colors.surfaceMuted, borderRadius: radius.pill, flex: 1, flexDirection: 'row', gap: spacing.xs, height: 36, paddingHorizontal: spacing.sm },
  dot: { backgroundColor: colors.brand, borderRadius: 4, height: 8, width: 8 },
  activeText: { color: colors.brandDeep, flex: 1, fontFamily: fonts.medium, fontSize: type.caption },
  modalRoot: { flex: 1, justifyContent: 'flex-end' },
  backdrop: { backgroundColor: colors.overlay, bottom: 0, left: 0, position: 'absolute', right: 0, top: 0 },
  sheet: { backgroundColor: colors.surface, borderTopLeftRadius: radius.sheet, borderTopRightRadius: radius.sheet, maxHeight: '78%', paddingHorizontal: spacing.md, paddingTop: spacing.xs, ...shadows.sheet },
  handle: { alignSelf: 'center', backgroundColor: colors.lineStrong, borderRadius: radius.pill, height: 4, marginBottom: spacing.sm, width: 40 },
  sheetHeader: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between', marginBottom: spacing.sm },
  sheetTitle: { color: colors.ink, fontFamily: fonts.bold, fontSize: type.section },
  closeButton: { alignItems: 'center', height: 44, justifyContent: 'center', width: 44 },
  option: { alignItems: 'center', borderBottomColor: colors.line, borderBottomWidth: 1, flexDirection: 'row', justifyContent: 'space-between', minHeight: 52, paddingVertical: spacing.sm },
  optionPressed: { backgroundColor: colors.surfaceMuted },
  optionText: { color: colors.inkSoft, fontFamily: fonts.regular, fontSize: type.body },
  optionTextActive: { color: colors.brandDeep, fontFamily: fonts.semibold },
  sheetActions: { flexDirection: 'row', gap: spacing.sm, paddingTop: spacing.md },
  flexButton: { flex: 1 },
});
