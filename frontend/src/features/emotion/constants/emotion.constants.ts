import type { EmotionLevel } from "../types/emotion.types";

export const emotionOptions: { value: EmotionLevel; label: string; emoji: string; color: string }[] = [
  { value: "VERY_HAPPY", label: "Rất vui", emoji: "😁", color: "#22c55e" },
  { value: "HAPPY", label: "Vui", emoji: "🙂", color: "#84cc16" },
  { value: "NEUTRAL", label: "Bình thường", emoji: "😐", color: "#facc15" },
  { value: "SAD", label: "Buồn", emoji: "😔", color: "#3b82f6" },
  { value: "STRESSED", label: "Căng thẳng", emoji: "😣", color: "#f43f5e" },
];
