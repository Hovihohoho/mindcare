import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Bell, LogOut, Menu, Search, X } from "lucide-react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { notificationApi } from "@/features/notifications";
import type { AuthUser } from "@/features/auth";
import { Avatar, Button, ConfirmDialog, Logo, cn } from "@/shared";
import { tokenStorage, userStorage } from "@/shared/lib/storage";
import { userNavigation } from "@/shared/constants/navigation";

const searchItems = [
  ...userNavigation,
  { label: "Hồ sơ cá nhân", to: "/profile" },
  { label: "Lịch sử cảm xúc", to: "/emotion/history" },
  { label: "Thư viện đánh giá", to: "/assessments/library" },
  { label: "Thông báo", to: "/notifications" },
];

export function UserHeader() {
  const [open, setOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [logoutOpen, setLogoutOpen] = useState(false);
  const [query, setQuery] = useState("");
  const navigate = useNavigate();
  const user = userStorage.get<AuthUser>();
  const isAuthenticated = Boolean(tokenStorage.get() && user);
  const unread = useQuery({
    queryKey: ["notifications", "unread"],
    queryFn: notificationApi.unreadCount,
    enabled: isAuthenticated,
    refetchInterval: 60_000,
  });
  const results = query.trim()
    ? searchItems.filter((item) => item.label.toLocaleLowerCase("vi").includes(query.toLocaleLowerCase("vi")))
    : searchItems;

  function logout() {
    tokenStorage.clear();
    setLogoutOpen(false);
    navigate("/login", { replace: true });
  }

  return (
    <>
      <header className="sticky top-0 z-40 border-b border-slate-200/60 bg-cyan-100/90 shadow-sm backdrop-blur-md">
        <div className="page-container flex h-20 items-center justify-between gap-6">
          <Logo />
          <nav className="hidden items-center gap-7 lg:flex" aria-label="Điều hướng chính">
            {userNavigation.map((item) => <NavLink key={item.to} to={item.to} className={({ isActive }) => cn("focus-ring rounded-lg py-2 text-[15px] transition", isActive ? "font-bold text-slate-700" : "text-slate-500 hover:text-brand-700")}>{item.label}</NavLink>)}
          </nav>
          <div className="hidden items-center gap-2 sm:flex">
            <button className="focus-ring rounded-full p-2 text-slate-500 hover:bg-white/60" aria-label="Tìm kiếm" onClick={() => setSearchOpen(true)}><Search className="size-5" /></button>
            <span className="mx-1 h-8 w-px bg-slate-300/40" />
            {isAuthenticated ? <>
              <Link className="focus-ring relative rounded-full p-2 text-slate-500 hover:bg-white/60" aria-label="Thông báo" to="/notifications"><Bell className="size-5" />{Boolean(unread.data) && <span className="absolute -right-0.5 -top-0.5 min-w-5 rounded-full bg-rose-600 px-1 text-center text-[10px] font-bold leading-5 text-white">{Math.min(unread.data ?? 0, 99)}</span>}</Link>
              <Link to="/profile"><Avatar className="ring-2 ring-sky-200" fallback={user?.fullName} /></Link>
              <button className="focus-ring rounded-full p-2 text-slate-500 hover:bg-white/60" onClick={() => setLogoutOpen(true)} aria-label="Đăng xuất"><LogOut className="size-5" /></button>
            </> : <>
              <Link to="/login"><Button className="px-4 text-brand-700" variant="ghost">Đăng nhập</Button></Link>
              <Link to="/register"><Button className="h-12 px-6">Đăng ký</Button></Link>
            </>}
          </div>
          <button className="focus-ring rounded-lg p-2 text-brand-700 lg:hidden" onClick={() => setOpen((value) => !value)} aria-label="Mở menu">{open ? <X /> : <Menu />}</button>
        </div>
        {open && <nav className="border-t border-line bg-white px-5 py-4 lg:hidden">{userNavigation.map(({ icon: Icon, ...item }) => <NavLink key={item.to} to={item.to} onClick={() => setOpen(false)} className="flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold text-slate-700 hover:bg-brand-50"><Icon className="size-4 text-brand-600" />{item.label}</NavLink>)}<button className="flex w-full items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold" onClick={() => setSearchOpen(true)}><Search className="size-4" />Tìm kiếm</button>{isAuthenticated && <Link className="flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold" to="/notifications"><Bell className="size-4" />Thông báo ({unread.data ?? 0})</Link>}<div className="mt-3 border-t pt-3">{isAuthenticated ? <Button className="w-full" variant="outline" onClick={() => setLogoutOpen(true)}>Đăng xuất</Button> : <Link to="/login"><Button className="w-full">Đăng nhập</Button></Link>}</div></nav>}
      </header>
      {searchOpen && <div className="fixed inset-0 z-[90] bg-slate-950/40 p-4 pt-[12vh]" onMouseDown={() => setSearchOpen(false)}><div className="mx-auto max-w-xl rounded-2xl bg-white p-4 shadow-2xl" onMouseDown={(event) => event.stopPropagation()}><div className="flex items-center gap-3 border-b border-line px-2 pb-3"><Search className="text-muted" /><input autoFocus className="h-11 flex-1 outline-none" placeholder="Tìm chức năng..." value={query} onChange={(event) => setQuery(event.target.value)} /><button onClick={() => setSearchOpen(false)}><X /></button></div><div className="max-h-80 overflow-y-auto py-2">{results.map((item) => <button className="block w-full rounded-xl px-4 py-3 text-left hover:bg-brand-50" key={item.to} onClick={() => { navigate(item.to); setSearchOpen(false); setQuery(""); }}>{item.label}</button>)}{!results.length && <p className="p-5 text-center text-sm text-muted">Không tìm thấy chức năng phù hợp.</p>}</div></div></div>}
      <ConfirmDialog open={logoutOpen} title="Đăng xuất khỏi MindCare?" description="Bạn sẽ cần đăng nhập lại để xem dữ liệu cá nhân." onCancel={() => setLogoutOpen(false)} onConfirm={logout} />
    </>
  );
}
