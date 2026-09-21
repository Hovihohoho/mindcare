import { StyleSheet, View } from 'react-native';
import { FilterButton } from '@/components/filter-controls';
import { SearchField } from '@/components/search-field';
import { spacing } from '@/theme/tokens';

type Props = {
  query: string;
  searchLabel: string;
  onQueryChange: (value: string) => void;
  onOpenFilter?: () => void;
  activeFilterLabel?: string;
};

export function ListToolbar({ query, searchLabel, onQueryChange, onOpenFilter, activeFilterLabel }: Props) {
  return (
    <View style={styles.toolbar}>
      <SearchField label={searchLabel} onChangeText={onQueryChange} value={query} />
      {onOpenFilter && <FilterButton activeLabel={activeFilterLabel} onPress={onOpenFilter} />}
    </View>
  );
}

const styles = StyleSheet.create({
  toolbar: { gap: spacing.xs, paddingHorizontal: spacing.md, paddingBottom: spacing.sm },
});
