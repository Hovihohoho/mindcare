import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, Circle, Target } from "lucide-react";
import { useState } from "react";
import { Button, Card, Loading, PageHeader, cn } from "@/shared";
import { carePlanApi, type CareGoal } from "../api/carePlan.api";
import { activityTemplates, goalLabels } from "../constants/carePlan.constants";

function localDate() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

export function CarePlanPage() {
  const queryClient = useQueryClient();
  const [goal, setGoal] = useState<CareGoal>("REDUCE_STRESS");
  const [editing, setEditing] = useState(false);
  const plan = useQuery({ queryKey: ["care-plan"], queryFn: carePlanApi.get, retry: false });
  const save = useMutation({ mutationFn: () => carePlanApi.save(goal, activityTemplates[goal]), onSuccess: (data) => { queryClient.setQueryData(["care-plan"], data); setEditing(false); } });
  const toggle = useMutation({
    mutationFn: ({ id, completed }: { id: string; completed: boolean }) => completed ? carePlanApi.undo(id, localDate()) : carePlanApi.complete(id, localDate()),
    onSuccess: (data) => queryClient.setQueryData(["care-plan"], data),
  });
  if (plan.isLoading) return <Loading />;
  if (!plan.data || editing) return <div className="space-y-8">
    <PageHeader title="Kế hoạch tự chăm sóc" description="Chọn một mục tiêu nhỏ để bắt đầu. Bạn có thể thay đổi kế hoạch sau." />
    <div className="grid gap-4 sm:grid-cols-2">{(Object.keys(goalLabels) as CareGoal[]).map((value) => <button key={value} onClick={() => setGoal(value)} className={cn("rounded-2xl border p-6 text-left transition", goal === value ? "border-brand-500 bg-brand-50 ring-2 ring-brand-100" : "border-line bg-white hover:border-brand-300")}><Target className="size-7 text-brand-600" /><b className="mt-4 block text-lg">{goalLabels[value]}</b><p className="mt-2 text-sm text-muted">{activityTemplates[value].length} hoạt động gợi ý mỗi tuần</p></button>)}</div>
    <Card className="p-6"><h2 className="font-bold">Hoạt động trong kế hoạch</h2><ul className="mt-4 space-y-3">{activityTemplates[goal].map((item) => <li className="flex items-center justify-between gap-4" key={item.activityCode}><span>{item.title}</span><span className="text-sm text-muted">{item.targetPerWeek} lần/tuần</span></li>)}</ul><Button className="mt-6" loading={save.isPending} onClick={() => save.mutate()}>Bắt đầu kế hoạch</Button></Card>
  </div>;

  const progress = plan.data.targetThisWeek === 0 ? 0 : Math.min(100, Math.round(plan.data.completedThisWeek * 100 / plan.data.targetThisWeek));
  return <div className="space-y-8">
    <PageHeader title="Kế hoạch tự chăm sóc" description={`Mục tiêu hiện tại: ${goalLabels[plan.data.goal]}`} />
    <Card className="p-7"><div className="flex items-end justify-between"><div><p className="text-sm text-muted">Tiến độ tuần này</p><p className="mt-2 text-3xl font-bold">{plan.data.completedThisWeek}/{plan.data.targetThisWeek}</p></div><b className="text-brand-700">{progress}%</b></div><div className="mt-5 h-3 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-brand-500 transition-all" style={{ width: `${progress}%` }} /></div></Card>
    <div className="space-y-4">{plan.data.activities.map((item) => <Card className="flex items-center justify-between gap-5 p-5" key={item.id}><div><h2 className="font-bold">{item.title}</h2><p className="mt-1 text-sm text-muted">Đã làm {item.completedThisWeek}/{item.targetPerWeek} lần trong tuần</p></div><button aria-label={item.completedToday ? "Bỏ đánh dấu hôm nay" : "Đánh dấu hoàn thành hôm nay"} disabled={toggle.isPending} onClick={() => toggle.mutate({ id: item.id, completed: item.completedToday })} className={cn("grid size-11 shrink-0 place-items-center rounded-full", item.completedToday ? "bg-emerald-500 text-white" : "bg-slate-100 text-slate-400")} >{item.completedToday ? <Check /> : <Circle />}</button></Card>)}</div>
    <Button variant="outline" onClick={() => { setGoal(plan.data.goal); setEditing(true); }}>Thiết lập lại kế hoạch</Button>
  </div>;
}
