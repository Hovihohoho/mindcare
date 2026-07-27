import { trendMock } from "../constants/emotion.constants";

export function EmotionTrendChart() {
  const labels = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"];
  return (
    <div className="mt-6 flex h-52 items-end gap-3 md:gap-6">
      {trendMock.map((point, index) => (
        <div className="flex h-full flex-1 flex-col justify-end text-center" key={point.bucketStart}>
          <div className={`mx-auto w-full max-w-16 rounded-t-xl ${["bg-lime-500","bg-lime-500","bg-blue-500","bg-yellow-400","bg-yellow-400","bg-green-500","bg-orange-500"][index]}`} style={{ height: `${point.averageScore * 18}%` }} title={`${point.averageScore}/5`} />
          <span className="mt-3 text-xs font-semibold text-muted">{labels[index]}</span>
        </div>
      ))}
    </div>
  );
}
