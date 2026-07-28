import { useEffect, useRef, useState } from "react";
import { Bookmark, LogOut, Settings, UserRound } from "lucide-react";
import { Link } from "react-router-dom";
import { Avatar, cn } from "@/shared";

interface UserAvatarMenuProps {
  fallback: string;
  onLogout: () => void;
}

const menuItems = [
  { label: "Hồ sơ cá nhân", to: "/profile", icon: UserRound },
  { label: "Đã lưu", to: "/bookmarks", icon: Bookmark },
  { label: "Cài đặt", to: "/settings", icon: Settings },
];

export function UserAvatarMenu({ fallback, onLogout }: UserAvatarMenuProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;

    const closeWhenClickingOutside = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const closeWithEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };

    document.addEventListener("mousedown", closeWhenClickingOutside);
    document.addEventListener("keydown", closeWithEscape);
    return () => {
      document.removeEventListener("mousedown", closeWhenClickingOutside);
      document.removeEventListener("keydown", closeWithEscape);
    };
  }, [open]);

  const logout = () => {
    setOpen(false);
    onLogout();
  };

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        className="focus-ring block rounded-full"
        aria-label="Mở menu tài khoản"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        <Avatar className="ring-2 ring-sky-200" fallback={fallback} />
      </button>

      <div
        role="menu"
        className={cn(
          "absolute right-0 top-[calc(100%+12px)] z-50 w-64 origin-top-right rounded-lg border border-neutral-300 bg-white py-2 shadow-xl transition",
          open
            ? "visible translate-y-0 scale-100 opacity-100"
            : "invisible -translate-y-1 scale-95 opacity-0",
        )}
      >
        {menuItems.map(({ icon: Icon, label, to }) => (
          <Link
            key={to}
            role="menuitem"
            to={to}
            onClick={() => setOpen(false)}
            className="flex items-center gap-4 px-4 py-3 text-base text-slate-800 transition hover:bg-slate-50 focus:bg-slate-50 focus:outline-none"
          >
            <Icon className="size-5 text-slate-500" />
            {label}
          </Link>
        ))}
        <div className="mx-4 my-1 h-px bg-neutral-300" />
        <button
          type="button"
          role="menuitem"
          onClick={logout}
          className="flex w-full items-center gap-4 px-4 py-3 text-left text-base text-red-500 transition hover:bg-red-50 focus:bg-red-50 focus:outline-none"
        >
          <LogOut className="size-5" />
          Đăng xuất
        </button>
      </div>
    </div>
  );
}
