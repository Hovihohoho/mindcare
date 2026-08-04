import { useQuery } from "@tanstack/react-query";
import { BrainCircuit, Command, Search, Stethoscope, X } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { assessmentApi } from "@/features/assessment/api/assessment.api";
import { expertApi } from "@/features/expert-directory/api/expert.api";
import { useCurrentUser } from "@/features/auth";
import { expertNavigation, userNavigation } from "@/shared/constants/navigation";

interface SearchResult {
  key: string;
  label: string;
  description: string;
  to: string;
  group: "Chức năng" | "Chuyên gia" | "Bài đánh giá";
}

const extraFunctions: SearchResult[] = [
  { key: "bookmark", label: "Nội dung đã lưu", description: "Chuyên gia và bài đánh giá đã bookmark", to: "/bookmarks", group: "Chức năng" },
  { key: "profile", label: "Hồ sơ cá nhân", description: "Xem và cập nhật thông tin tài khoản", to: "/profile", group: "Chức năng" },
];

const expertExtraFunctions: SearchResult[] = [
  { key: "expert-calendar", label: "Lịch làm việc chuyên gia", description: "Tạo và quản lý khung giờ tư vấn", to: "/expert/calendar", group: "Chức năng" },
];

function normalize(value: string) {
  return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("vi");
}

export function HeaderSearch() {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [debounced, setDebounced] = useState("");
  const [active, setActive] = useState(0);
  const input = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();
  const currentUser = useCurrentUser();
  const isExpert = currentUser.data?.role === "ROLE_EXPERT";

  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(query.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [query]);
  useEffect(() => {
    const shortcut = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setOpen((value) => !value);
      }
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("keydown", shortcut);
    return () => document.removeEventListener("keydown", shortcut);
  }, []);
  useEffect(() => {
    if (open) window.setTimeout(() => input.current?.focus(), 0);
  }, [open]);

  const enabled = debounced.length >= 2;
  const experts = useQuery({
    queryKey: ["header-search-experts", debounced],
    queryFn: () => expertApi.list({ keyword: debounced }),
    enabled,
  });
  const assessments = useQuery({
    queryKey: ["header-search-assessments"],
    queryFn: assessmentApi.list,
    enabled,
    staleTime: 5 * 60_000,
  });
  const results = useMemo<SearchResult[]>(() => {
    if (!enabled) return [];
    const expected = normalize(debounced);
    const roleNavigation = isExpert
      ? [...userNavigation, ...expertNavigation]
      : [...userNavigation];
    const roleExtraFunctions = isExpert
      ? [...extraFunctions, ...expertExtraFunctions]
      : extraFunctions;
    const functions: SearchResult[] = [
      ...roleNavigation
        .map((item): SearchResult => ({ key: `function-${item.to}`, label: item.label, description: "Đi tới chức năng", to: item.to, group: "Chức năng" })),
      ...roleExtraFunctions,
    ]
      .filter((item) => normalize(`${item.label} ${item.description}`).includes(expected));
    const expertResults = (experts.data?.items ?? []).map((item) => ({
      key: `expert-${item.expertUserId}`, label: item.displayName, description: item.headline,
      to: `/experts/${item.expertUserId}`, group: "Chuyên gia" as const,
    }));
    const assessmentResults = (assessments.data ?? [])
      .filter((item) => normalize(`${item.title} ${item.description} ${item.code}`).includes(expected))
      .map((item) => ({
        key: `assessment-${item.code}`, label: item.title, description: item.description,
        to: `/assessments/${item.code}`, group: "Bài đánh giá" as const,
      }));
    return [...functions, ...expertResults, ...assessmentResults].slice(0, 12);
  }, [assessments.data, debounced, enabled, experts.data, isExpert]);

  useEffect(() => setActive(0), [debounced]);
  const choose = (result: SearchResult) => {
    const expertOnlyRoute = result.to === "/expert" || result.to.startsWith("/expert/");
    if (expertOnlyRoute && !isExpert) return;
    setOpen(false);
    setQuery("");
    navigate(result.to);
  };
  const loading = enabled && (experts.isLoading || assessments.isLoading);

  return (
    <>
      <button className="focus-ring rounded-full p-2 text-slate-500 hover:bg-white/50" aria-label="Tìm kiếm (Ctrl+K)" onClick={() => setOpen(true)}><Search className="size-5" /></button>
      {open && <div className="fixed inset-0 z-[70] bg-slate-950/40 p-3 pt-[10vh] backdrop-blur-sm sm:p-6" role="dialog" aria-modal="true" aria-label="Tìm kiếm toàn hệ thống" onMouseDown={(event) => {
        if (event.target === event.currentTarget) setOpen(false);
      }}>
        <div className="mx-auto max-w-2xl overflow-hidden rounded-2xl border border-white/30 bg-white shadow-2xl">
          <div className="flex items-center gap-3 border-b border-line px-4">
            <Search className="size-5 shrink-0 text-brand-700" />
            <input ref={input} className="h-16 min-w-0 flex-1 bg-transparent text-base outline-none" placeholder="Tìm chức năng, chuyên gia, bài đánh giá..." value={query} onChange={(event) => setQuery(event.target.value)} onKeyDown={(event) => {
              if (event.key === "ArrowDown") { event.preventDefault(); setActive((value) => Math.min(value + 1, results.length - 1)); }
              if (event.key === "ArrowUp") { event.preventDefault(); setActive((value) => Math.max(value - 1, 0)); }
              if (event.key === "Enter" && results[active]) choose(results[active]);
            }} />
            <span className="hidden items-center gap-1 rounded-md border border-line px-2 py-1 text-xs text-muted sm:flex"><Command className="size-3" />K</span>
            <button className="rounded-full p-2 hover:bg-slate-100" onClick={() => setOpen(false)} aria-label="Đóng tìm kiếm"><X className="size-5" /></button>
          </div>
          <div className="max-h-[65vh] overflow-y-auto p-2">
            {query.trim().length < 2 && <p className="px-4 py-10 text-center text-sm text-muted">Nhập ít nhất 2 ký tự để tìm kiếm.</p>}
            {loading && <p className="px-4 py-10 text-center text-sm text-muted">Đang tìm kiếm...</p>}
            {!loading && enabled && results.length === 0 && <p className="px-4 py-10 text-center text-sm text-muted">Không tìm thấy kết quả phù hợp.</p>}
            {results.map((result, index) => (
              <button key={result.key} className={`flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left ${index === active ? "bg-brand-50" : "hover:bg-slate-50"}`} onMouseEnter={() => setActive(index)} onClick={() => choose(result)}>
                <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-white text-brand-700 shadow-sm">{result.group === "Chuyên gia" ? <Stethoscope className="size-4" /> : result.group === "Bài đánh giá" ? <BrainCircuit className="size-4" /> : <Search className="size-4" />}</span>
                <span className="min-w-0"><span className="block truncate text-sm font-bold text-slate-800">{result.label}</span><span className="block truncate text-xs text-muted">{result.group} · {result.description}</span></span>
              </button>
            ))}
          </div>
        </div>
      </div>}
    </>
  );
}
