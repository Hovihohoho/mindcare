/* Hallmark · component: emotion face · genre: playful · theme: MindCare
 * states: default · selected · pressed · disabled · loading · error · success
 * pre-emit critique: P5 H5 E4 S5 R5 V5
 */
import { StyleSheet, View } from 'react-native';

import { colors } from '@/theme/tokens';
import type { EmotionFace } from './emotion.constants';

export function EmotionFaceIcon({ face, color, size = 36 }: { face: EmotionFace; color: string; size?: number }) {
  const eyeTop = size * 0.31;
  const eyeSize = Math.max(3, size * 0.1);
  const mouthWidth = size * 0.42;
  return (
    <View accessibilityElementsHidden importantForAccessibility="no-hide-descendants" style={[styles.face, { backgroundColor: color, height: size, width: size, borderRadius: size / 2 }]}>
      {face === 'stressed' ? (
        <>
          <CrossEye left={size * 0.25} size={eyeSize + 2} top={eyeTop} />
          <CrossEye left={size * 0.62} size={eyeSize + 2} top={eyeTop} />
        </>
      ) : face === 'laugh' ? (
        <>
          <LaughEye left={size * 0.22} size={eyeSize + 4} top={eyeTop} />
          <LaughEye left={size * 0.61} size={eyeSize + 4} top={eyeTop} />
        </>
      ) : (
        <>
          <View style={[styles.eye, { height: eyeSize, left: size * 0.27, top: eyeTop, width: eyeSize, borderRadius: eyeSize / 2 }]} />
          <View style={[styles.eye, { height: eyeSize, right: size * 0.27, top: eyeTop, width: eyeSize, borderRadius: eyeSize / 2 }]} />
        </>
      )}
      <View style={[
        styles.mouth,
        { left: (size - mouthWidth) / 2, width: mouthWidth },
        face === 'laugh' && { backgroundColor: colors.emotionFaceInk, borderRadius: size, height: size * 0.22, top: size * 0.54 },
        face === 'smile' && { borderBottomWidth: 2, borderBottomColor: colors.emotionFaceInk, borderRadius: size, height: size * 0.2, top: size * 0.49 },
        face === 'neutral' && { backgroundColor: colors.emotionFaceInk, borderRadius: 2, height: 2, top: size * 0.62 },
        (face === 'sad' || face === 'stressed') && { borderTopWidth: 2, borderTopColor: colors.emotionFaceInk, borderRadius: size, height: size * 0.18, top: size * 0.59 },
      ]} />
    </View>
  );
}

function LaughEye({ left, size, top }: { left: number; size: number; top: number }) {
  return (
    <View style={{ height: size, left, position: 'absolute', top, width: size }}>
      <View style={[styles.laughLine, { left: 0, transform: [{ rotate: '-38deg' }], width: size * 0.62 }]} />
      <View style={[styles.laughLine, { right: 0, transform: [{ rotate: '38deg' }], width: size * 0.62 }]} />
    </View>
  );
}

function CrossEye({ left, size, top }: { left: number; size: number; top: number }) {
  return (
    <View style={{ height: size, left, position: 'absolute', top, width: size }}>
      <View style={[styles.crossLine, { width: size, transform: [{ rotate: '45deg' }] }]} />
      <View style={[styles.crossLine, { width: size, transform: [{ rotate: '-45deg' }] }]} />
    </View>
  );
}

const styles = StyleSheet.create({
  face: { position: 'relative' },
  eye: { backgroundColor: colors.emotionFaceInk, position: 'absolute' },
  laughLine: { backgroundColor: colors.emotionFaceInk, borderRadius: 1, height: 2, position: 'absolute', top: 2 },
  mouth: { position: 'absolute' },
  crossLine: { backgroundColor: colors.emotionFaceInk, borderRadius: 1, height: 2, left: 0, position: 'absolute', top: 2 },
});
