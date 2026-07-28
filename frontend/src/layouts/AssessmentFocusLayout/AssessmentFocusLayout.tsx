import { X } from "lucide-react";
import { Link, Outlet } from "react-router-dom";
import { Logo } from "@/shared";

export function AssessmentFocusLayout() {
  return (
    <div className="min-h-screen bg-[#f7f9fb]">
      <header className="relative flex h-24 items-center justify-center border-b border-line bg-white shadow-sm">
        <Link className="absolute left-6 flex items-center gap-2 text-slate-500 md:left-16" to="/assessments"><X className="size-6" />Thoát</Link>
        <Logo />
      </header>
      <main className="px-5 py-8 md:py-10"><Outlet /></main>
      <footer className="py-10 text-center text-sm text-slate-400">© {new Date().getFullYear()} MindCare. Thông tin của bạn được bảo mật tuyệt đối.</footer>
    </div>
  );
}
