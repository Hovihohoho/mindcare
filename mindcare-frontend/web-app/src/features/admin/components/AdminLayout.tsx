import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { Bell, BrainCircuit, FileText, LayoutDashboard, LogOut, ScrollText, UserCheck, Users } from "lucide-react";
import { authApi } from "@/features/auth";
import { Button, Logo, cn } from "@/shared";
import { tokenStorage } from "@/shared/lib/storage";

const links = [
  ["/admin", "Dashboard", LayoutDashboard],
  ["/admin/users", "Người dùng", Users],
  ["/admin/experts", "Duyệt chuyên gia", UserCheck],
  ["/admin/content", "Nội dung", FileText],
  ["/admin/ai-documents", "Tài liệu AI", BrainCircuit],
  ["/admin/notifications", "Thông báo", Bell],
  ["/admin/audit", "Nhật ký", ScrollText],
] as const;

export function AdminLayout() {
  const navigate = useNavigate();

  async function logout() {
    if (!window.confirm("Bạn có chắc muốn đăng xuất không?")) return;
    try {
      await authApi.logout();
    } catch {
      // Local logout must still complete if the server session already expired.
    } finally {
      tokenStorage.clear();
      navigate("/login", { replace: true });
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 lg:grid lg:grid-cols-[260px_1fr]">
      <aside className="border-r bg-white p-5">
        <Logo />
        <nav className="mt-8 space-y-2">
          {links.map(([to, label, Icon]) => (
            <NavLink className={({ isActive }) => cn("flex items-center gap-3 rounded-xl px-4 py-3 font-semibold", isActive ? "bg-brand-50 text-brand-700" : "text-slate-600")} end={to === "/admin"} key={to} to={to}>
              <Icon className="size-5" />{label}
            </NavLink>
          ))}
        </nav>
        <Button className="mt-8 w-full" leftIcon={<LogOut className="size-4" />} onClick={logout} variant="outline">Đăng xuất</Button>
      </aside>
      <main className="p-5 md:p-8"><Outlet /></main>
    </div>
  );
}
