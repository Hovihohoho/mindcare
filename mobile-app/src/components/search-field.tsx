import Ionicons from '@expo/vector-icons/Ionicons';
import { forwardRef, useState } from 'react';
import { Pressable, StyleSheet, TextInput, View, type TextInputProps } from 'react-native';
import { colors, fonts, radius, spacing, type } from '@/theme/tokens';

type Props = TextInputProps & { label: string };

export const SearchField = forwardRef<TextInput, Props>(({ label, value, onChangeText, ...props }, ref) => {
  const [focused, setFocused] = useState(false);
  return (
    <View style={[styles.container, focused && styles.focused]}>
      <Ionicons color={focused ? colors.brand : colors.muted} name="search-outline" size={20} />
      <TextInput
        ref={ref}
        accessibilityLabel={label}
        clearButtonMode="while-editing"
        onBlur={(event) => { setFocused(false); props.onBlur?.(event); }}
        onFocus={(event) => { setFocused(true); props.onFocus?.(event); }}
        onChangeText={onChangeText}
        placeholder={label}
        placeholderTextColor={colors.muted}
        returnKeyType="search"
        style={styles.input}
        value={value}
        {...props}
      />
      {!!value && (
        <Pressable accessibilityLabel="Xóa nội dung tìm kiếm" hitSlop={8} onPress={() => onChangeText?.('')} style={styles.clear}>
          <Ionicons color={colors.muted} name="close-circle" size={20} />
        </Pressable>
      )}
    </View>
  );
});

SearchField.displayName = 'SearchField';

const styles = StyleSheet.create({
  container: { alignItems: 'center', backgroundColor: colors.surface, borderColor: colors.line, borderRadius: radius.input, borderWidth: 1, flexDirection: 'row', gap: spacing.xs, height: 48, paddingHorizontal: spacing.sm },
  focused: { borderColor: colors.brand },
  input: { color: colors.ink, flex: 1, fontFamily: fonts.regular, fontSize: type.body, height: 46, paddingVertical: 0 },
  clear: { alignItems: 'center', height: 44, justifyContent: 'center', width: 44 },
});
