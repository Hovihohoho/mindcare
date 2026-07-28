import { useState } from "react";
import { Bell, LogOut, Menu, Search, X } from "lucide-react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { Avatar, Button, Logo, cn } from "@/shared";
import type { AuthUser } from "@/features/auth";
import { tokenStorage, userStorage } from "@/shared/lib/storage";
import { userNavigation } from "@/shared/constants/navigation";

export function UserHeader() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const user = userStorage.get<AuthUser>();
  const isAuthenticated = Boolean(tokenStorage.get() && user);
  const logout = () => { tokenStorage.clear(); navigate("/login"); };

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200/60 bg-cyan-100/80 shadow-sm backdrop-blur-md">
      <div className="page-container flex h-20 items-center justify-between gap-6">
        <Logo />
        <nav className="hidden items-center gap-7 lg:flex" aria-label="Điều hướng chính">
          {userNavigation.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn("focus-ring rounded-lg py-2 text-[15px] transition", isActive ? "font-bold text-slate-600" : "font-normal text-slate-500 hover:text-brand-700")
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="hidden items-center gap-2 sm:flex">
          <button className="focus-ring rounded-full p-2 text-slate-500 hover:bg-white/50" aria-label="Tìm kiếm"><Search className="size-5" /></button>
          <span className="mx-1 h-8 w-px bg-slate-300/40" />
          {isAuthenticated ? (
            <>
              <button className="focus-ring rounded-full p-2 text-slate-500 hover:bg-white/50" aria-label="Thông báo"><Bell className="size-5" /></button>
              <Link to="/profile"><Avatar className="ring-2 ring-sky-200" fallback={user?.fullName} /></Link>
              <button className="focus-ring rounded-full p-2 text-slate-500 hover:bg-white/50" onClick={logout} aria-label="Đăng xuất"><LogOut className="size-5" /></button>
            </>
          ) : (
            <>
              <Link to="/login"><Button className="px-4 text-brand-700" variant="ghost">Đăng nhập</Button></Link>
              <Link to="/register"><Button className="h-12 px-6">Đăng ký</Button></Link>
            </>
          )}
        </div>
        <button className="focus-ring rounded-lg p-2 text-brand-700 lg:hidden" onClick={() => setOpen((value) => !value)} aria-label="Mở menu">
          {open ? <X /> : <Menu />}
        </button>
      </div>
      {open && (
        <nav className="border-t border-line bg-white px-5 py-4 lg:hidden">
          {userNavigation.map(({ icon: Icon, ...item }) => (
            <NavLink key={item.to} to={item.to} onClick={() => setOpen(false)} className="flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold text-slate-700 hover:bg-brand-50">
              <Icon className="size-4 text-brand-600" />{item.label}
            </NavLink>
          ))}
          <div className="mt-3 flex gap-2 border-t border-line pt-3">{isAuthenticated ? <Button className="w-full" variant="outline" onClick={logout}>Đăng xuất</Button> : <><Link className="flex-1" to="/login"><Button className="w-full" variant="outline">Đăng nhập</Button></Link><Link className="flex-1" to="/register"><Button className="w-full">Đăng ký</Button></Link></>}</div>
        </nav>
      )}
    </header>
  );
}
