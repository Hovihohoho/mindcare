import { Link } from "react-router-dom";
import { AuthFormShell } from "../components/AuthFormShell";

export function ForgotPasswordPage() {
  return (
    <AuthFormShell title="Quên mật khẩu" description="Khôi phục quyền truy cập tài khoản.">
      <p className="rounded-2xl bg-amber-50 p-5 text-sm leading-6 text-amber-800">Auth Service chưa triển khai API quên mật khẩu. Hệ thống không giả lập việc gửi email.</p>
      <Link className="mt-6 block text-center text-sm font-semibold text-brand-600" to="/login">Quay lại đăng nhập</Link>
    </AuthFormShell>
  );
}
