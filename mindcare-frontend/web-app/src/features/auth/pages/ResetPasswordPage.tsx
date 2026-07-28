import { Link } from "react-router-dom";
import { AuthFormShell } from "../components/AuthFormShell";

export function ResetPasswordPage() {
  return (
    <AuthFormShell title="Đặt lại mật khẩu" description="Thiết lập mật khẩu mới.">
      <p className="rounded-2xl bg-amber-50 p-5 text-sm leading-6 text-amber-800">Auth Service chưa triển khai API đặt lại mật khẩu.</p>
      <Link className="mt-6 block text-center text-sm font-semibold text-brand-600" to="/login">Quay lại đăng nhập</Link>
    </AuthFormShell>
  );
}
