import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Button, Input } from "@/shared";
import { AuthFormShell } from "../components/AuthFormShell";
import { authApi } from "../api/auth.api";

function errorMessage(error: unknown) {
  return (error as { response?: { data?: { message?: string } } })
    .response?.data?.message ?? "Không thể đặt lại mật khẩu. Vui lòng yêu cầu liên kết mới.";
}

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [done, setDone] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const token = params.get("token")?.trim() ?? "";

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    if (password !== confirmation) {
      setError("Mật khẩu xác nhận không khớp.");
      return;
    }
    if (!/[A-Za-zÀ-ỹ]/.test(password) || !/\d/.test(password)) {
      setError("Mật khẩu phải có ít nhất một chữ cái và một chữ số.");
      return;
    }
    setLoading(true);
    try {
      await authApi.resetPassword(token, password);
      setDone(true);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthFormShell title="Đặt lại mật khẩu" description="Mật khẩu mới cần 8–72 ký tự, gồm ít nhất một chữ cái và một chữ số.">
      {!token ? (
        <div className="space-y-4">
          <p role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">Liên kết không hợp lệ vì thiếu mã đặt lại mật khẩu.</p>
          <Link to="/forgot-password"><Button className="w-full">Yêu cầu liên kết mới</Button></Link>
        </div>
      ) : done ? (
        <div className="space-y-4">
          <p className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-700">Đổi mật khẩu thành công. Tất cả phiên đăng nhập cũ đã được đăng xuất.</p>
          <Link to="/login"><Button className="w-full">Đăng nhập</Button></Link>
        </div>
      ) : (
        <form className="space-y-4" onSubmit={submit}>
          {error && <p role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
          <Input label="Mật khẩu mới" minLength={8} maxLength={72} onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
          <Input label="Xác nhận mật khẩu" minLength={8} maxLength={72} onChange={(event) => setConfirmation(event.target.value)} required type="password" value={confirmation} />
          <Button className="w-full" loading={loading}>Đặt lại mật khẩu</Button>
        </form>
      )}
    </AuthFormShell>
  );
}
