import { emotionOptions, type EmotionOption } from "../constants/emotion.constants";

export function emotionOptionFromScore(score: number | null): EmotionOption | undefined {
  if (score === null || !Number.isFinite(score)) return undefined;
  if (score >= 2) return emotionOptions.find((item) => item.value === "VERY_HAPPY");
  if (score >= 1) return emotionOptions.find((item) => item.value === "HAPPY");
  if (score >= 0) return emotionOptions.find((item) => item.value === "NEUTRAL");
  if (score >= -1) return emotionOptions.find((item) => item.value === "SAD");
  return emotionOptions.find((item) => item.value === "STRESSED");
}

export function dateKey(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}
