import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Bell, Menu, Search, X } from "lucide-react";
import { Link, NavLink } from "react-router-dom";
import { Button, Logo, cn } from "@/shared";
import { userNavigation } from "@/shared/constants/navigation";
import { useCurrentUser } from "@/features/auth";
import { notificationApi } from "@/features/notifications";
import { UserAvatarMenu } from "./UserAvatarMenu";

export function UserHeader() {
  const [open, setOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [search, setSearch] = useState("");
  const isAuthenticated = Boolean(localStorage.getItem("mindcare.accessToken"));
  const currentUser = useCurrentUser();
  const unread = useQuery({
    queryKey: ["notification-unread-count"],
    queryFn: notificationApi.unreadCount,
    enabled: isAuthenticated,
    refetchInterval: 30_000,
  });
  const searchResults = userNavigation.filter((item) =>
    item.label.toLocaleLowerCase("vi").includes(search.toLocaleLowerCase("vi")),
  );

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
        <div className="hidden items-center gap-2 sm:flex">
          <div className="relative">
            <button className="focus-ring rounded-full p-2 text-slate-500 hover:bg-white/50" aria-label="Tìm kiếm" onClick={() => setSearchOpen((value) => !value)}>
              <Search className="size-5" />
            </button>
            {searchOpen && (
              <div className="absolute right-0 top-12 w-80 rounded-xl border bg-white p-3 shadow-xl">
                <input autoFocus className="h-10 w-full rounded-lg border px-3 text-sm outline-none focus:border-brand-500" onChange={(e) => setSearch(e.target.value)} placeholder="Tìm chức năng..." value={search} />
                <div className="mt-2 max-h-64 overflow-auto">
                  {searchResults.map((item) => <Link className="block rounded-lg px-3 py-2 text-sm hover:bg-brand-50" key={item.to} onClick={() => setSearchOpen(false)} to={item.to}>{item.label}</Link>)}
                  {search.trim() && <Link className="block rounded-lg px-3 py-2 text-sm font-semibold text-brand-700 hover:bg-brand-50" onClick={() => setSearchOpen(false)} to={`/experts?keyword=${encodeURIComponent(search)}`}>Tìm chuyên gia “{search}”</Link>}
                </div>
              </div>
            )}
          </div>
          <span className="mx-1 h-8 w-px bg-slate-300/40" />
          {isAuthenticated ? (
            <>
              <Link className="focus-ring relative rounded-full p-2 text-slate-500 hover:bg-white/50" aria-label="Thông báo" to="/notifications">
                <Bell className="size-5" />
                {!!unread.data && <span className="absolute -right-1 -top-1 min-w-5 rounded-full bg-rose-500 px-1 text-center text-[11px] font-bold leading-5 text-white">{unread.data > 99 ? "99+" : unread.data}</span>}
              </Link>
              <UserAvatarMenu avatarUrl={currentUser.data?.avatarUrl ?? undefined} fallback={currentUser.data?.fullName ?? "MT"} />
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
          {isAuthenticated ? (
            <Link className="flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold" onClick={() => setOpen(false)} to="/notifications"><Bell className="size-4" />Thông báo ({unread.data ?? 0})</Link>
          ) : (
            <div className="mt-3 flex gap-2 border-t border-line pt-3">
              <Link className="flex-1" to="/login"><Button className="w-full" variant="outline">Đăng nhập</Button></Link>
              <Link className="flex-1" to="/register"><Button className="w-full">Đăng ký</Button></Link>
            </div>
          )}
        </nav>
      )}
    </header>
  );
}
