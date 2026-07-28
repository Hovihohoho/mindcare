import { BookOpenText, LogOut, UsersRound } from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { Logo, cn } from "@/shared";
import { tokenStorage } from "@/shared/lib/storage";

export function AdminLayout() {
  const navigate = useNavigate();
  const logout = () => {
    tokenStorage.clear();
    navigate("/login", { replace: true });
  };
  return (
    <div className="min-h-screen bg-slate-50 lg:grid lg:grid-cols-[250px_1fr]">
      <aside className="border-r border-line bg-white p-5">
        <Logo to="/admin" />
        <nav className="mt-10 space-y-2">
          {[["/admin/users", "Tài khoản", UsersRound], ["/admin/ai-documents", "Tài liệu AI", BookOpenText]].map(([to, label, Icon]) => (
            <NavLink key={String(to)} to={String(to)} className={({ isActive }) => cn("flex items-center gap-3 rounded-xl px-4 py-3 font-semibold", isActive ? "bg-brand-50 text-brand-700" : "text-slate-600 hover:bg-slate-50")}><Icon className="size-5" />{String(label)}</NavLink>
          ))}
        </nav>
        <button className="mt-8 flex items-center gap-3 px-4 py-3 text-sm font-semibold text-rose-700" onClick={logout}><LogOut className="size-4" />Đăng xuất</button>
      </aside>
      <main className="min-w-0 p-5 md:p-8"><Outlet /></main>
    </div>
  );
}
