import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, Circle, ExternalLink, Sparkles, Target } from "lucide-react";
import { useMemo, useState } from "react";
import { Button, Card, Loading, PageHeader, cn } from "@/shared";
import { carePlanApi } from "../api/carePlan.api";
import { goalLabels } from "../constants/carePlan.constants";

function localDate() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

export function CarePlanPage() {
  const queryClient = useQueryClient();
  const [selectedCode, setSelectedCode] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const plan = useQuery({ queryKey: ["care-plan"], queryFn: carePlanApi.get, retry: false });
  const templates = useQuery({ queryKey: ["care-plan-templates"], queryFn: carePlanApi.templates });
  const recommendations = useQuery({ queryKey: ["care-plan-recommendations"], queryFn: carePlanApi.recommendations });
  const recommendedCodes = useMemo(() => new Set(recommendations.data?.map((item) => item.templateCode) ?? []), [recommendations.data]);
  const effectiveSelectedCode = selectedCode ?? recommendations.data?.[0]?.templateCode ?? plan.data?.templateCode;
  const selected = templates.data?.find((item) => item.templateCode === effectiveSelectedCode) ?? templates.data?.[0];

  const apply = useMutation({
    mutationFn: (templateCode: string) => carePlanApi.applyTemplate(templateCode),
    onSuccess: (data) => {
      queryClient.setQueryData(["care-plan"], data);
      setEditing(false);
    },
  });
  const toggle = useMutation({
    mutationFn: ({ id, completed }: { id: string; completed: boolean }) => completed
      ? carePlanApi.undo(id, localDate())
      : carePlanApi.complete(id, localDate()),
    onSuccess: (data) => queryClient.setQueryData(["care-plan"], data),
  });

  if (plan.isLoading || templates.isLoading) return <Loading />;
  if (!plan.data || editing) return (
    <div className="space-y-8">
      <PageHeader title="Kế hoạch tự chăm sóc" description="Chọn một form cố định đã được kiểm duyệt. Hệ thống chỉ đề xuất dựa trên benchmark, không tự tạo hoạt động mới." />

      {!!recommendations.data?.length && (
        <Card className="border-amber-200 bg-amber-50 p-6">
          <div className="flex items-start gap-3"><Sparkles className="mt-0.5 size-5 text-amber-700" /><div>
            <h2 className="font-bold text-amber-950">Đề xuất từ dữ liệu 7 ngày</h2>
            {recommendations.data.map((item) => (
              <div className="mt-3" key={item.templateCode}>
                <p className="text-sm leading-6 text-amber-900">{item.message}</p>
                <a className="mt-1 inline-flex items-center gap-1 text-xs font-semibold text-amber-800 underline" href={item.benchmarkSourceUrl} rel="noreferrer" target="_blank">Xem benchmark <ExternalLink className="size-3" /></a>
              </div>
            ))}
          </div></div>
        </Card>
      )}

      <div className="grid gap-4 sm:grid-cols-2">
        {templates.data?.map((template) => (
          <button key={template.templateCode} onClick={() => setSelectedCode(template.templateCode)} className={cn("rounded-2xl border p-6 text-left transition", selected?.templateCode === template.templateCode ? "border-brand-500 bg-brand-50 ring-2 ring-brand-100" : "border-line bg-white hover:border-brand-300")}>
            <div className="flex items-start justify-between gap-3"><Target className="size-7 text-brand-600" />{recommendedCodes.has(template.templateCode) && <span className="rounded-full bg-amber-100 px-2 py-1 text-xs font-bold text-amber-800">Được đề xuất</span>}</div>
            <b className="mt-4 block text-lg">{template.title}</b>
            <p className="mt-2 text-sm leading-6 text-muted">{template.description}</p>
            <p className="mt-3 text-xs text-muted">{template.activities.length} hoạt động · phiên bản {template.templateVersion}</p>
          </button>
        ))}
      </div>

      {selected && <Card className="p-6">
        <h2 className="font-bold">Hoạt động trong kế hoạch</h2>
        <ul className="mt-4 space-y-3">{selected.activities.map((item) => <li className="flex items-center justify-between gap-4" key={item.activityCode}><span>{item.title}</span><span className="text-sm text-muted">{item.targetPerWeek} lần/tuần</span></li>)}</ul>
        <div className="mt-5 rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-600">
          <p>{selected.limitation}</p>
          <div className="mt-2 flex flex-wrap gap-4"><a className="font-semibold text-brand-700 underline" href={selected.sourceUrl} rel="noreferrer" target="_blank">Nguồn benchmark</a><a className="font-semibold text-brand-700 underline" href={selected.implementationSourceUrl} rel="noreferrer" target="_blank">Hướng dẫn thực hiện</a></div>
        </div>
        <Button className="mt-6" loading={apply.isPending} onClick={() => apply.mutate(selected.templateCode)}>Áp dụng kế hoạch</Button>
      </Card>}
    </div>
  );

  const progress = plan.data.targetThisWeek === 0 ? 0 : Math.min(100, Math.round(plan.data.completedThisWeek * 100 / plan.data.targetThisWeek));
  return <div className="space-y-8">
    <PageHeader title="Kế hoạch tự chăm sóc" description={`Mục tiêu hiện tại: ${goalLabels[plan.data.goal]}`} />
    <Card className="p-7"><div className="flex items-end justify-between"><div><p className="text-sm text-muted">Tiến độ tuần này</p><p className="mt-2 text-3xl font-bold">{plan.data.completedThisWeek}/{plan.data.targetThisWeek}</p></div><b className="text-brand-700">{progress}%</b></div><div className="mt-5 h-3 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-brand-500 transition-all" style={{ width: `${progress}%` }} /></div>{plan.data.sourceUrl && <a className="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-brand-700 underline" href={plan.data.sourceUrl} rel="noreferrer" target="_blank">Nguồn của kế hoạch <ExternalLink className="size-4" /></a>}</Card>
    <div className="space-y-4">{plan.data.activities.map((item) => <Card className="flex items-center justify-between gap-5 p-5" key={item.id}><div><h2 className="font-bold">{item.title}</h2><p className="mt-1 text-sm text-muted">Đã làm {item.completedThisWeek}/{item.targetPerWeek} lần trong tuần</p></div><button aria-label={item.completedToday ? "Bỏ đánh dấu hôm nay" : "Đánh dấu hoàn thành hôm nay"} disabled={toggle.isPending} onClick={() => toggle.mutate({ id: item.id, completed: item.completedToday })} className={cn("grid size-11 shrink-0 place-items-center rounded-full", item.completedToday ? "bg-emerald-500 text-white" : "bg-slate-100 text-slate-400")}>{item.completedToday ? <Check /> : <Circle />}</button></Card>)}</div>
    <Button variant="outline" onClick={() => { setSelectedCode(plan.data.templateCode); setEditing(true); }}>Thiết lập lại kế hoạch</Button>
  </div>;
}
