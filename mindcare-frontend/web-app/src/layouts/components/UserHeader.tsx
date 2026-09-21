import { useState } from "react";
import { Menu, X } from "lucide-react";
import { Link, NavLink } from "react-router-dom";
import { Button, Logo, cn } from "@/shared";
import { userNavigation } from "@/shared/constants/navigation";
import { useCurrentUser } from "@/features/auth";
import { UserAvatarMenu } from "./UserAvatarMenu";
import { HeaderSearch } from "@/features/search";
import { NotificationPopover } from "@/features/notification";

export function UserHeader() {
  const [open, setOpen] = useState(false);
  const isAuthenticated = Boolean(localStorage.getItem("mindcare.accessToken"));
  const currentUser = useCurrentUser();

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200/60 bg-cyan-100/80 shadow-sm backdrop-blur-md">
      <div className="page-container flex h-20 items-center justify-between gap-6">
        <Logo />
        <nav className="hidden items-center gap-7 lg:flex" aria-label="Điều hướng chính">
          {userNavigation.map((item) => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) =>
              cn("focus-ring rounded-lg py-2 text-[15px] transition", isActive ? "font-bold text-slate-600" : "text-slate-500 hover:text-brand-700")
            }>{item.label}</NavLink>
          ))}
        </nav>
        <div className="flex items-center gap-1 sm:gap-2">
          <HeaderSearch />
          <span className="mx-1 h-8 w-px bg-slate-300/40" />
          {isAuthenticated ? (
            <>
              <NotificationPopover />
              <span className="hidden sm:block"><UserAvatarMenu avatarUrl={currentUser.data?.avatarUrl ?? undefined} fallback={currentUser.data?.fullName ?? "MT"} /></span>
            </>
          ) : (
            <>
              <Link className="hidden sm:block" to="/login"><Button className="px-4 text-brand-700" variant="ghost">Đăng nhập</Button></Link>
              <Link className="hidden sm:block" to="/register"><Button className="h-12 px-6">Đăng ký</Button></Link>
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
          {!isAuthenticated && <div className="mt-3 flex gap-2 border-t border-line pt-3">
            <Link className="flex-1" to="/login"><Button className="w-full" variant="outline">Đăng nhập</Button></Link>
            <Link className="flex-1" to="/register"><Button className="w-full">Đăng ký</Button></Link>
          </div>}
        </nav>
      )}
    </header>
  );
}
