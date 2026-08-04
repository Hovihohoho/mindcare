import type { EmotionLevel } from "../types/emotion.types";

export type EmotionFace = "laugh" | "smile" | "neutral" | "sad" | "stressed";

export interface EmotionOption {
  value: EmotionLevel;
  label: string;
  shortLines: readonly string[];
  face: EmotionFace;
  colorClass: string;
  surfaceClass: string;
  ringClass: string;
}

export const emotionOptions: EmotionOption[] = [
  {
    value: "VERY_HAPPY",
    label: "Rất vui",
    shortLines: ["Rất vui"],
    face: "laugh",
    colorClass: "bg-green-500",
    surfaceClass: "bg-green-50",
    ringClass: "ring-green-500",
  },
  {
    value: "HAPPY",
    label: "Vui",
    shortLines: ["Vui"],
    face: "smile",
    colorClass: "bg-lime-500",
    surfaceClass: "bg-lime-50",
    ringClass: "ring-lime-500",
  },
  {
    value: "NEUTRAL",
    label: "Bình thường",
    shortLines: ["Bình", "thường"],
    face: "neutral",
    colorClass: "bg-blue-500",
    surfaceClass: "bg-blue-50",
    ringClass: "ring-blue-500",
  },
  {
    value: "SAD",
    label: "Buồn",
    shortLines: ["Buồn"],
    face: "sad",
    colorClass: "bg-yellow-400",
    surfaceClass: "bg-yellow-50",
    ringClass: "ring-yellow-400",
  },
  {
    value: "STRESSED",
    label: "Căng thẳng",
    shortLines: ["Căng", "thẳng"],
    face: "stressed",
    colorClass: "bg-rose-500",
    surfaceClass: "bg-rose-50",
    ringClass: "ring-rose-500",
  },
];
