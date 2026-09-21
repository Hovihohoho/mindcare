import { useState } from "react";
import { Button, Input } from "@/shared";
import { AuthFormShell } from "../components/AuthFormShell";
import { authApi } from "../api/auth.api";

function errorMessage(error: unknown) {
  return (error as { response?: { data?: { message?: string } } })
    .response?.data?.message ?? "Không thể gửi email. Vui lòng thử lại sau.";
}

export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setSent(false);
    setError(null);
    try {
      await authApi.forgotPassword(email.trim());
      setSent(true);
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthFormShell title="Quên mật khẩu" description="MindCare sẽ gửi liên kết đặt lại mật khẩu qua email.">
      <form onSubmit={submit} className="space-y-4">
        <Input label="Email" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
        {sent && <p role="status" className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-700">Nếu email tồn tại, liên kết đặt lại mật khẩu đã được gửi. Hãy kiểm tra cả thư rác.</p>}
        {error && <p role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
        <Button className="w-full" loading={loading}>Gửi liên kết</Button>
      </form>
    </AuthFormShell>
  );
}
