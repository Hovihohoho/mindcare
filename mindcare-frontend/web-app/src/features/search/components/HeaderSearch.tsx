import { useQuery } from "@tanstack/react-query";
import { BrainCircuit, Search, X } from "lucide-react";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { assessmentApi } from "@/features/assessment/api/assessment.api";
import { userNavigation } from "@/shared/constants/navigation";

function normalize(value: string) {
  return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("vi");
}

export function HeaderSearch() {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const navigate = useNavigate();
  const assessments = useQuery({ queryKey: ["header-search-assessments"], queryFn: assessmentApi.list, enabled: open, staleTime: 300_000 });
  const results = useMemo(() => {
    const expected = normalize(query.trim());
    if (expected.length < 2) return [];
    const functions = userNavigation.filter((item) => normalize(item.label).includes(expected))
      .map((item) => ({ key: item.to, label: item.label, to: item.to, assessment: false }));
    const tests = (assessments.data ?? []).filter((item) => normalize(`${item.title} ${item.description} ${item.code}`).includes(expected))
      .map((item) => ({ key: item.code, label: item.title, to: `/assessments/${item.code}`, assessment: true }));
    return [...functions, ...tests].slice(0, 12);
  }, [assessments.data, query]);
  const choose = (to: string) => { setOpen(false); setQuery(""); navigate(to); };
  return <>
    <button className="focus-ring rounded-full p-2 text-slate-500 hover:bg-white/50" aria-label="Tìm kiếm" onClick={() => setOpen(true)}><Search className="size-5" /></button>
    {open && <div className="fixed inset-0 z-[70] bg-slate-950/40 p-6 pt-[10vh]" role="dialog" aria-modal="true">
      <div className="mx-auto max-w-2xl rounded-2xl bg-white p-4 shadow-2xl">
        <div className="flex items-center gap-3"><Search className="size-5 text-brand-700" /><input autoFocus className="h-12 min-w-0 flex-1 outline-none" placeholder="Tìm chức năng, bài đánh giá..." value={query} onChange={(event) => setQuery(event.target.value)} /><button onClick={() => setOpen(false)} aria-label="Đóng"><X className="size-5" /></button></div>
        <div className="mt-2 space-y-1">{results.map((result) => <button key={result.key} className="flex w-full items-center gap-3 rounded-xl p-3 text-left hover:bg-brand-50" onClick={() => choose(result.to)}>{result.assessment ? <BrainCircuit className="size-4 text-brand-700" /> : <Search className="size-4 text-brand-700" />}<span className="font-semibold">{result.label}</span></button>)}</div>
      </div>
    </div>}
  </>;
}
