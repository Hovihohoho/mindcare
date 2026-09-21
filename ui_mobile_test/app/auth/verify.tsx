import { useRouter } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';
import { DetailScreen, PrimaryAction } from '../../src/components/secondary-ui';
import { palette, space, type } from '../../src/theme/tokens';
export default function VerifyScreen() { const router = useRouter(); return <DetailScreen title="Xác minh email"><View style={styles.body}><Text style={styles.title}>Kiểm tra hộp thư của bạn</Text><Text style={styles.text}>Chúng tôi đã gửi một liên kết xác minh mẫu. Trong prototype này, bạn có thể tiếp tục mà không cần mở email.</Text><PrimaryAction label="Tôi đã xác minh" onPress={() => router.replace('/(tabs)/home')} /><Text style={styles.note}>Không nhận được email? Gửi lại sau 00:45</Text></View></DetailScreen>; }
const styles = StyleSheet.create({ body: { paddingTop: space.xl }, title: { color: palette.ink, fontSize: 28, fontWeight: '700', letterSpacing: -.7 }, text: { color: palette.muted, fontSize: type.body, lineHeight: 24, marginBottom: space.xl, marginTop: space.sm }, note: { color: palette.muted, fontSize: 12, marginTop: space.md, textAlign: 'center' } });
