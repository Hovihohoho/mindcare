import { useMemo, useState } from "react";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { Activity, AlertTriangle, BedDouble, ExternalLink, Footprints, HeartPulse, Info, RefreshCw, Timer, Trash2 } from "lucide-react";
import { Button, Card, Loading, PageHeader, cn } from "@/shared";
import { healthApi } from "../api/health.api";
import { HealthTrendChart } from "../components/HealthTrendChart";
import type { HealthMetricRecord, HealthMetricType, HealthTrendPoint } from "../types/health.types";

const metricDefinitions = [
  { type: "STEP_COUNT", label: "Bước chân", shortLabel: "Bước", icon: Footprints, unit: "bước" },
  { type: "HEART_RATE", label: "Nhịp tim trung bình", shortLabel: "Nhịp tim", icon: HeartPulse, unit: "bpm" },
  { type: "SPO2", label: "Độ bão hòa oxy", shortLabel: "SpO₂", icon: HeartPulse, unit: "%" },
  { type: "SLEEP_SESSION", label: "Thời gian ngủ", shortLabel: "Giấc ngủ", icon: BedDouble, unit: "giờ" },
  { type: "EXERCISE_SESSION", label: "Thời gian vận động", shortLabel: "Vận động", icon: Activity, unit: "phút" },
] as const;

function metricValue(type: HealthMetricType, points: HealthTrendPoint[]) {
  if (points.length === 0) return null;
  if (type === "HEART_RATE" || type === "SPO2") {
    const count = points.reduce((sum, point) => sum + point.count, 0);
    return count === 0 ? null : points.reduce((sum, point) => sum + point.value * point.count, 0) / count;
  }
  const total = points.reduce((sum, point) => sum + point.value, 0);
  return type === "EXERCISE_SESSION" ? total * 60 : total;
}

function formatMetricValue(type: HealthMetricType, value: number) {
  if (type === "STEP_COUNT") return Math.round(value).toLocaleString("vi-VN");
  if (type === "HEART_RATE" || type === "SPO2" || type === "EXERCISE_SESSION") return Math.round(value).toLocaleString("vi-VN");
  return value.toLocaleString("vi-VN", { maximumFractionDigits: 1, minimumFractionDigits: 1 });
}

function recordValue(record: HealthMetricRecord) {
  if (record.value !== null) return `${formatMetricValue(record.metricType, record.value)} ${record.unit ?? ""}`.trim();
  if (!record.startTime || !record.endTime) return "—";
  const minutes = Math.max(0, (Date.parse(record.endTime) - Date.parse(record.startTime)) / 60_000);
  return record.metricType === "EXERCISE_SESSION"
    ? `${Math.round(minutes)} phút`
    : `${(minutes / 60).toLocaleString("vi-VN", { maximumFractionDigits: 1 })} giờ`;
}

function sourceLabel(record: HealthMetricRecord) {
  return record.sourceName || record.dataOrigin || "Health Connect";
}

export function HealthDataPage() {
  const [days, setDays] = useState<7 | 30>(7);
  const [selectedMetric, setSelectedMetric] = useState<HealthMetricType>("STEP_COUNT");
  const queryClient = useQueryClient();
  const range = useMemo(() => {
    const to = new Date();
    const from = new Date(to.getTime() - days * 86_400_000);
    return { from: from.toISOString(), to: to.toISOString() };
  }, [days]);

  const trendQueries = useQueries({
    queries: metricDefinitions.map((metric) => ({
      queryKey: ["health-trends", metric.type, days, range.from, range.to],
      queryFn: () => healthApi.trends(metric.type, range.from, range.to),
      staleTime: 5 * 60_000,
    })),
  });
  const history = useQuery({
    queryKey: ["health-history", selectedMetric, days, range.from, range.to],
    queryFn: () => healthApi.history(selectedMetric, range.from, range.to),
    staleTime: 5 * 60_000,
  });
  const sourceSummary = useQuery({ queryKey: ["health-source-summary"], queryFn: healthApi.sourceSummary });
  const benchmarkEvaluations = useQuery({
    queryKey: ["health-benchmark-evaluations"],
    queryFn: async () => { await healthApi.analyzeAlerts(); return healthApi.benchmarkEvaluations(); },
    staleTime: 5 * 60_000,
  });
  const activeWarnings = benchmarkEvaluations.data?.filter((item) => item.status === "BELOW_BENCHMARK" || item.status === "RECHECK_RECOMMENDED") ?? [];
  const deleteSourceData = useMutation({
    mutationFn: healthApi.deleteHealthConnectData,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["health-source-summary"] });
      await queryClient.invalidateQueries({ queryKey: ["health-trends"] });
      await queryClient.invalidateQueries({ queryKey: ["health-history"] });
    },
  });
  const selectedIndex = metricDefinitions.findIndex((metric) => metric.type === selectedMetric);
  const selectedDefinition = metricDefinitions[selectedIndex];
  const selectedTrend = trendQueries[selectedIndex];
  const selectedPoints = selectedTrend.data ?? [];
  const recentRecords = history.data?.items ?? [];
  const refreshing = trendQueries.some((query) => query.isFetching) || history.isFetching;

  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: ["health-trends"] });
    void queryClient.invalidateQueries({ queryKey: ["health-history"] });
  };

  return (
    <div className="min-w-0 space-y-6">
      <PageHeader
        title="Dữ liệu sức khỏe"
        description="Các chỉ số đã được đồng bộ từ Health Connect trên thiết bị Android của bạn."
        actions={(
          <div className="flex flex-wrap items-center gap-2">
            <div className="flex rounded-full border border-line bg-white p-1" aria-label="Khoảng thời gian">
              {([7, 30] as const).map((value) => (
                <button
                  className={cn("focus-ring rounded-full px-4 py-2 text-sm font-semibold transition", days === value ? "bg-brand-700 text-white" : "text-slate-600 hover:bg-slate-100")}
                  key={value}
                  onClick={() => setDays(value)}
                  type="button"
                >
                  {value} ngày
                </button>
              ))}
            </div>
            <Button leftIcon={<RefreshCw className="size-4" />} loading={refreshing} onClick={refresh} variant="outline">Làm mới</Button>
          </div>
        )}
      />

      <div className="flex items-start gap-3 rounded-xl border border-sky-200 bg-sky-50 p-4 text-sm leading-6 text-sky-900">
        <Info className="mt-0.5 size-5 shrink-0" />
        <p>Trang web chỉ hiển thị dữ liệu đã đồng bộ. Cấp quyền, chỉnh quyền và “Đồng bộ ngay” được thực hiện trong ứng dụng MindCare trên Android.</p>
      </div>

      {!!activeWarnings.length && <section className="space-y-3" aria-label="Cảnh báo theo benchmark">
        {activeWarnings.map((warning) => <Card className="border-amber-200 bg-amber-50 p-5" key={warning.policyKey}>
          <div className="flex items-start gap-3"><AlertTriangle className="mt-0.5 size-5 shrink-0 text-amber-700" /><div><h2 className="font-bold text-amber-950">Cần lưu ý: {warning.metricType}</h2><p className="mt-1 text-sm leading-6 text-amber-900">{warning.message}</p><a className="mt-2 inline-flex items-center gap-1 text-xs font-semibold text-amber-800 underline" href={warning.sourceUrl} rel="noreferrer" target="_blank">{warning.sourceTitle} · policy {warning.policyVersion}<ExternalLink className="size-3" /></a></div></div>
        </Card>)}
      </section>}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Tổng quan sức khỏe">
        {metricDefinitions.map((metric, index) => {
          const query = trendQueries[index];
          const value = query.data ? metricValue(metric.type, query.data) : null;
          const Icon = metric.icon;
          return (
            <Card className="p-5" key={metric.type}>
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-sm font-medium text-slate-500">{metric.label}</p>
                  {query.isLoading ? <div className="mt-4 h-8 w-24 animate-pulse rounded bg-slate-100" /> : query.isError ? (
                    <p className="mt-3 text-sm font-semibold text-rose-600">Không tải được</p>
                  ) : value === null ? (
                    <p className="mt-2 text-2xl font-bold text-slate-400">—</p>
                  ) : (
                    <p className="mt-2 text-2xl font-bold text-slate-800">{formatMetricValue(metric.type, value)} <span className="text-sm font-semibold text-slate-500">{metric.unit}</span></p>
                  )}
                </div>
                <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700"><Icon className="size-5" /></span>
              </div>
              <p className="mt-4 text-xs text-slate-500">Trong {days} ngày gần nhất</p>
            </Card>
          );
        })}
      </section>

      <div className="border-b border-slate-200">
        <div className="flex gap-1 overflow-x-auto" role="tablist" aria-label="Loại chỉ số">
          {metricDefinitions.map((metric) => (
            <button
              aria-selected={selectedMetric === metric.type}
              className={cn("focus-ring shrink-0 border-b-2 px-4 py-3 text-sm font-semibold transition", selectedMetric === metric.type ? "border-brand-600 text-brand-700" : "border-transparent text-slate-500 hover:text-slate-800")}
              key={metric.type}
              onClick={() => setSelectedMetric(metric.type)}
              role="tab"
              type="button"
            >
              {metric.shortLabel}
            </button>
          ))}
        </div>
      </div>

      <div className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,1.35fr)_minmax(22rem,0.65fr)]">
        <Card className="min-w-0 p-5 sm:p-6">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 className="text-lg font-bold text-slate-800">Xu hướng {selectedDefinition.shortLabel.toLowerCase()}</h2>
              <p className="mt-1 text-sm text-slate-500">Tổng hợp theo ngày, múi giờ Việt Nam.</p>
            </div>
            <Timer className="size-5 text-brand-600" />
          </div>
          <div className="mt-5">
            {selectedTrend.isLoading ? <div className="grid h-64 place-items-center"><Loading /></div> : selectedTrend.isError ? (
              <div className="grid min-h-64 place-items-center rounded-xl bg-rose-50 p-6 text-center">
                <div><p className="font-semibold text-rose-700">Không thể tải xu hướng</p><Button className="mt-4" onClick={() => void selectedTrend.refetch()} size="sm" variant="outline">Thử lại</Button></div>
              </div>
            ) : selectedPoints.length === 0 ? (
              <div className="grid min-h-64 place-items-center rounded-xl bg-slate-50 p-6 text-center"><div><p className="font-semibold text-slate-700">Chưa có dữ liệu</p><p className="mt-2 text-sm text-slate-500">Mở ứng dụng Android và đồng bộ Health Connect.</p></div></div>
            ) : (
              <HealthTrendChart points={selectedPoints} valueFormatter={(value) => `${formatMetricValue(selectedMetric, selectedMetric === "EXERCISE_SESSION" ? value * 60 : value)} ${selectedDefinition.unit}`} />
            )}
          </div>
        </Card>

        <Card className="overflow-hidden">
          <div className="border-b border-slate-200 px-5 py-5">
            <h2 className="text-lg font-bold text-slate-800">Bản ghi gần đây</h2>
            <p className="mt-1 text-sm text-slate-500">Tối đa 50 bản ghi trong khoảng đã chọn.</p>
          </div>
          <div className="max-h-[25rem] overflow-y-auto">
            {history.isLoading ? <div className="grid min-h-52 place-items-center"><Loading /></div> : history.isError ? (
              <div className="p-5 text-sm text-rose-700">Không thể tải danh sách bản ghi.</div>
            ) : recentRecords.length === 0 ? (
              <div className="grid min-h-52 place-items-center p-5 text-center text-sm text-slate-500">Chưa có bản ghi {selectedDefinition.shortLabel.toLowerCase()}.</div>
            ) : recentRecords.map((record) => (
              <article className="flex items-start justify-between gap-4 border-b border-slate-100 px-5 py-4 last:border-b-0" key={record.id}>
                <div className="min-w-0">
                  <p className="font-semibold text-slate-800">{recordValue(record)}</p>
                  <p className="mt-1 truncate text-xs text-slate-500" title={sourceLabel(record)}>{sourceLabel(record)}</p>
                </div>
                <time className="shrink-0 text-right text-xs leading-5 text-slate-500" dateTime={record.recordedAt}>
                  {new Date(record.recordedAt).toLocaleDateString("vi-VN")}<br />
                  {new Date(record.recordedAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}
                </time>
              </article>
            ))}
          </div>
        </Card>
      </div>
      <Card className="border-rose-200 p-6">
        <div className="flex flex-wrap items-start justify-between gap-5">
          <div className="max-w-2xl">
            <h2 className="text-lg font-bold text-slate-800">Quản lý dữ liệu Health Connect trên MindCare</h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">{sourceSummary.isLoading ? "Đang kiểm tra dữ liệu đã lưu…" : `MindCare đang lưu ${sourceSummary.data?.recordCount ?? 0} bản ghi. Xóa tại đây sẽ dừng nhận dữ liệu mới cho tới khi bạn kết nối lại trong ứng dụng Android.`}</p>
            {sourceSummary.data?.oldestRecordAt && <p className="mt-1 text-xs text-muted">Khoảng dữ liệu: {new Date(sourceSummary.data.oldestRecordAt).toLocaleDateString("vi-VN")} – {new Date(sourceSummary.data.newestRecordAt!).toLocaleDateString("vi-VN")}</p>}
          </div>
          <Button disabled={!sourceSummary.data?.recordCount} leftIcon={<Trash2 className="size-4" />} loading={deleteSourceData.isPending} onClick={() => {
            if (window.confirm("Xóa vĩnh viễn toàn bộ dữ liệu Health Connect đã lưu trên MindCare? Hành động này không thể hoàn tác.")) deleteSourceData.mutate();
          }} variant="outline">Xóa dữ liệu đã đồng bộ</Button>
        </div>
        {deleteSourceData.isSuccess && <p className="mt-4 text-sm font-semibold text-emerald-700">Đã xóa dữ liệu Health Connect trên MindCare và tắt đồng bộ phía máy chủ.</p>}
        {deleteSourceData.isError && <p className="mt-4 text-sm font-semibold text-rose-700">Không thể xóa dữ liệu lúc này. Vui lòng thử lại.</p>}
      </Card>
    </div>
  );
}
