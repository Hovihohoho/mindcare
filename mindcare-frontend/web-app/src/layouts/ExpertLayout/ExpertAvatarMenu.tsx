import { useEffect, useRef, useState } from "react";
import { ChevronDown, LogOut, Settings, UserRound } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import { Avatar, cn } from "@/shared";
import { tokenStorage } from "@/shared/lib/storage";
import { authApi } from "@/features/auth";

interface ExpertAvatarMenuProps {
  displayName: string;
}

export function ExpertAvatarMenu({ displayName }: ExpertAvatarMenuProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

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

  const logout = async () => {
    if (!window.confirm("Bạn có chắc muốn đăng xuất không?")) return;
    try {
      await authApi.logout();
    } catch {
      // Local logout must still complete if the server session already expired.
    } finally {
      tokenStorage.clear();
      queryClient.clear();
      setOpen(false);
      navigate("/login", { replace: true });
    }
  };

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        className="focus-ring flex items-center gap-2 rounded-full p-1"
        aria-label="Mở menu tài khoản chuyên gia"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        <Avatar className="size-8" fallback={displayName} />
        <ChevronDown className={cn("size-4 text-slate-600 transition", open && "rotate-180")} />
      </button>

      <div
        role="menu"
        className={cn(
          "absolute right-0 top-[calc(100%+10px)] z-50 w-56 origin-top-right rounded-lg border border-neutral-300 bg-white py-2 shadow-xl transition",
          open
            ? "visible translate-y-0 scale-100 opacity-100"
            : "invisible -translate-y-1 scale-95 opacity-0",
        )}
      >
        <Link
          role="menuitem"
          to="/expert/profile"
          onClick={() => setOpen(false)}
          className="flex items-center gap-3 px-4 py-3 text-sm text-slate-700 hover:bg-slate-50 focus:bg-slate-50 focus:outline-none"
        >
          <UserRound className="size-5 text-slate-500" />
          Hồ sơ chuyên gia
        </Link>
        <Link
          role="menuitem"
          to="/expert/settings"
          onClick={() => setOpen(false)}
          className="flex items-center gap-3 px-4 py-3 text-sm text-slate-700 hover:bg-slate-50 focus:bg-slate-50 focus:outline-none"
        >
          <Settings className="size-5 text-slate-500" />
          Cài đặt
        </Link>
        <div className="mx-4 my-1 h-px bg-neutral-300" />
        <button
          type="button"
          role="menuitem"
          onClick={logout}
          className="flex w-full items-center gap-3 px-4 py-3 text-left text-sm text-red-500 hover:bg-red-50 focus:bg-red-50 focus:outline-none"
        >
          <LogOut className="size-5" />
          Đăng xuất
        </button>
      </div>
    </div>
  );
}
