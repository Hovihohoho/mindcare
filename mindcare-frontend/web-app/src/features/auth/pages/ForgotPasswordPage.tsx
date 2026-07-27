import { useState } from "react";
import { Link } from "react-router-dom";
import { Button, Input } from "@/shared";
import { AuthFormShell } from "../components/AuthFormShell";

export function ForgotPasswordPage() {
  const [sent, setSent] = useState(false);
  return (
    <AuthFormShell title="Quên mật khẩu" description="Vui lòng nhập email đã đăng ký để nhận mã xác thực thiết lập lại mật khẩu.">
      {sent ? (
        <div className="rounded-2xl bg-emerald-50 p-5 text-sm leading-6 text-emerald-800">Đường dẫn đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư của bạn.</div>
      ) : (
        <form className="space-y-5" onSubmit={(event) => { event.preventDefault(); setSent(true); }}>
          <Input label="Email" type="email" placeholder="example@email.com" required />
          <Button className="w-full" size="lg">Gửi mã xác thực</Button>
        </form>
      )}
      <Link className="mt-6 block text-center text-sm font-semibold text-brand-600" to="/login">Quay lại đăng nhập</Link>
    </AuthFormShell>
  );
}
