import { useQuery } from "@tanstack/react-query";
import { Activity, ArrowDownRight, ArrowRight, ArrowUpRight, ClipboardCheck, Footprints, HeartPulse, Moon, Target } from "lucide-react";
import { Link } from "react-router-dom";
import { Card, Loading, PageHeader } from "@/shared";
import { progressApi } from "../api/progress.api";

function total(points: { value: number }[] | undefined) { return points?.reduce((sum, point) => sum + point.value, 0) ?? 0; }
function checkIns(points: { count: number }[]) { return points.reduce((sum, point) => sum + point.count, 0); }
function format(value: number, digits = 0) { return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: digits }).format(value); }

function Change({ current, previous }: { current: number; previous: number }) {
  if (!previous) return <span className="text-xs text-muted">Chưa đủ dữ liệu tuần trước</span>;
  const percent = Math.round(((current - previous) / previous) * 100);
  const Icon = percent > 0 ? ArrowUpRight : percent < 0 ? ArrowDownRight : ArrowRight;
  return <span className={percent > 0 ? "text-xs font-semibold text-emerald-700" : "text-xs font-semibold text-muted"}><Icon className="mr-1 inline size-4" />{Math.abs(percent)}% so với tuần trước</span>;
}

export function ProgressPage() {
  const query = useQuery({ queryKey: ["weekly-progress"], queryFn: progressApi.get });
  if (query.isLoading) return <Loading />;
  if (query.isError || !query.data) return <Card className="p-8"><h1 className="text-xl font-bold">Chưa tải được tiến triển</h1><p className="mt-2 text-muted">Hãy thử lại sau ít phút. Dữ liệu gốc của bạn không bị ảnh hưởng.</p></Card>;
  const data = query.data;
  const currentCheckIns = checkIns(data.currentEmotion);
  const previousCheckIns = checkIns(data.previousEmotion);
  const steps = total(data.currentHealth.STEP_COUNT);
  const previousSteps = total(data.previousHealth.STEP_COUNT);
  const sleepMinutes = total(data.currentHealth.SLEEP_SESSION);
  const exerciseMinutes = total(data.currentHealth.EXERCISE_SESSION);
  const planPercent = data.carePlan?.targetThisWeek ? Math.min(100, Math.round(data.carePlan.completedThisWeek * 100 / data.carePlan.targetThisWeek)) : 0;
  const cards = [
    { label: "Lần check-in", value: format(currentCheckIns), icon: HeartPulse, change: <Change current={currentCheckIns} previous={previousCheckIns} />, href: "/emotion" },
    { label: "Bước chân", value: format(steps), icon: Footprints, change: <Change current={steps} previous={previousSteps} />, href: "/health" },
    { label: "Thời lượng ngủ", value: sleepMinutes ? `${format(sleepMinutes / 60, 1)} giờ` : "Chưa có", icon: Moon, href: "/health" },
    { label: "Vận động", value: exerciseMinutes ? `${format(exerciseMinutes)} phút` : "Chưa có", icon: Activity, href: "/health" },
  ];
  return <div className="space-y-8">
    <PageHeader eyebrow="7 ngày gần nhất" title="Tiến triển của bạn" description="Một góc nhìn nhẹ nhàng từ những dữ liệu bạn đã chủ động ghi nhận. Đây không phải chẩn đoán hay điểm số sức khỏe." />
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{cards.map(({ label, value, icon: Icon, change, href }) => <Link to={href} key={label}><Card className="h-full p-5 transition hover:-translate-y-0.5 hover:border-brand-300"><div className="flex items-center justify-between"><span className="text-sm font-semibold text-muted">{label}</span><span className="grid size-10 place-items-center rounded-xl bg-brand-50 text-brand-700"><Icon className="size-5" /></span></div><p className="mt-5 text-2xl font-extrabold text-slate-900">{value}</p><div className="mt-2 min-h-5">{change ?? <span className="text-xs text-muted">Trong tuần này</span>}</div></Card></Link>)}</div>
    <div className="grid gap-6 lg:grid-cols-2">
      <Card className="p-6"><div className="flex items-start justify-between gap-4"><div><p className="text-sm font-semibold text-brand-700">Kế hoạch tự chăm sóc</p><h2 className="mt-1 text-xl font-bold">{data.carePlan ? `${data.carePlan.completedThisWeek}/${data.carePlan.targetThisWeek} hoạt động` : "Chưa thiết lập kế hoạch"}</h2></div><Target className="size-6 text-brand-600" /></div>{data.carePlan ? <><div className="mt-6 h-3 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-brand-500" style={{ width: `${planPercent}%` }} /></div><p className="mt-3 text-sm text-muted">Bạn đã hoàn thành {planPercent}% mục tiêu tuần này.</p></> : <p className="mt-4 text-sm leading-6 text-muted">Bắt đầu bằng vài hoạt động nhỏ, phù hợp với mục tiêu hiện tại của bạn.</p>}<Link className="mt-5 inline-flex items-center gap-2 text-sm font-bold text-brand-700" to="/care-plan">{data.carePlan ? "Xem kế hoạch" : "Tạo kế hoạch"}<ArrowRight className="size-4" /></Link></Card>
      <Card className="p-6"><div className="flex items-start justify-between gap-4"><div><p className="text-sm font-semibold text-brand-700">Sàng lọc gần nhất</p><h2 className="mt-1 text-xl font-bold">{data.latestAssessment ? `${data.latestAssessment.assessmentCode} · ${data.latestAssessment.interpretationLevel.replaceAll("_", " ")}` : "Chưa có kết quả"}</h2></div><ClipboardCheck className="size-6 text-brand-600" /></div><p className="mt-4 text-sm leading-6 text-muted">{data.latestAssessment ? `Thực hiện ngày ${new Date(data.latestAssessment.createdAt).toLocaleDateString("vi-VN")}. Kết quả chỉ mang ý nghĩa sàng lọc và theo dõi.` : "Bạn có thể thực hiện một bài sàng lọc phù hợp khi cảm thấy sẵn sàng."}</p><Link className="mt-5 inline-flex items-center gap-2 text-sm font-bold text-brand-700" to="/assessments">Xem bài đánh giá<ArrowRight className="size-4" /></Link></Card>
    </div>
    {!currentCheckIns && !steps && !sleepMinutes && !exerciseMinutes && <Card className="border-dashed p-6 text-center"><h2 className="font-bold">Tuần này chưa có nhiều dữ liệu</h2><p className="mt-2 text-sm text-muted">Hãy bắt đầu bằng một lần check-in cảm xúc. Mỗi ghi nhận nhỏ giúp bức tranh tiến triển rõ hơn.</p><Link className="mt-4 inline-flex font-bold text-brand-700" to="/emotion">Check-in ngay</Link></Card>}
  </div>;
}
