import { Link } from "react-router-dom";
import { Button } from "@/shared";
import { userStorage } from "@/shared/lib/storage";
import type { AuthUser } from "@/features/auth";

export function ForbiddenPage() {
  const role = userStorage.get<AuthUser>()?.role;
  const destination = role === "ROLE_ADMIN" ? "/admin" : "/";
  return (
    <main className="grid min-h-screen place-items-center bg-slate-50 px-5 text-center">
      <div>
        <p className="text-8xl font-black text-brand-100">403</p>
        <h1 className="mt-3 text-3xl font-extrabold">Bạn không có quyền truy cập</h1>
        <p className="mt-3 text-muted">Tài khoản hiện tại không được phép mở trang này.</p>
        <Link to={destination}><Button className="mt-7">Về trang phù hợp</Button></Link>
      </div>
    </main>
  );
}
