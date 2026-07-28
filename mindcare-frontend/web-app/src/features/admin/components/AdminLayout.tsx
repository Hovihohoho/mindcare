import { useState } from "react";
import { BellRing, BookOpenText, LayoutDashboard, LogOut, UsersRound } from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { ConfirmDialog, Logo, cn } from "@/shared";
import { tokenStorage } from "@/shared/lib/storage";

const navigation = [
  ["/admin", "Tổng quan", LayoutDashboard, true],
  ["/admin/users", "Tài khoản", UsersRound, false],
  ["/admin/ai-documents", "Dữ liệu AI", BookOpenText, false],
  ["/admin/notifications", "Gửi thông báo", BellRing, false],
] as const;

export function AdminLayout() {
  const navigate = useNavigate();
  const [confirming, setConfirming] = useState(false);
  function logout() {
    tokenStorage.clear();
    navigate("/login", { replace: true });
  }
  return (
    <div className="min-h-screen bg-slate-50 lg:grid lg:grid-cols-[270px_1fr]">
      <aside className="border-r border-line bg-slate-950 p-5 text-white">
        <Logo className="rounded-xl bg-white p-3" to="/admin" />
        <p className="mt-7 px-4 text-xs font-bold uppercase tracking-widest text-slate-500">Quản trị hệ thống</p>
        <nav className="mt-3 space-y-2">{navigation.map(([to, label, Icon, end]) => <NavLink end={end} key={to} to={to} className={({ isActive }) => cn("flex items-center gap-3 rounded-xl px-4 py-3 font-semibold transition", isActive ? "bg-brand-600 text-white" : "text-slate-300 hover:bg-slate-800")}><Icon className="size-5" />{label}</NavLink>)}</nav>
        <button className="mt-8 flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold text-rose-300 hover:bg-slate-800" onClick={() => setConfirming(true)}><LogOut className="size-4" />Đăng xuất</button>
      </aside>
      <main className="min-w-0 p-5 md:p-8"><Outlet /></main>
      <ConfirmDialog open={confirming} title="Đăng xuất trang quản trị?" description="Phiên quản trị hiện tại sẽ kết thúc." onCancel={() => setConfirming(false)} onConfirm={logout} />
    </div>
  );
}
