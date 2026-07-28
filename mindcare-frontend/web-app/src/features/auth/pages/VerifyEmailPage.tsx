import { useState, type FormEvent } from "react";
import axios from "axios";
import { MailCheck } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { Button, Input } from "@/shared";
import { authApi } from "../api/auth.api";
import { AuthFormShell } from "../components/AuthFormShell";

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const [email, setEmail] = useState(params.get("email") ?? "");
  const [code, setCode] = useState("");
  const [message, setMessage] = useState("Nhập mã gồm 6 chữ số đã được gửi đến email đăng ký.");
  const [error, setError] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [resending, setResending] = useState(false);
  const [success, setSuccess] = useState(false);

  async function verify(event: FormEvent) {
    event.preventDefault();
    setError("");
    setVerifying(true);
    try {
      await authApi.verifyEmail(email.trim(), code);
      setSuccess(true);
      setMessage("Email đã được xác thực thành công. Bạn có thể đăng nhập.");
    } catch (caught) {
      const detail = axios.isAxiosError<{ message?: string }>(caught)
        ? caught.response?.data?.message
        : undefined;
      setError(detail || "Mã xác thực không đúng hoặc đã hết hạn.");
    } finally {
      setVerifying(false);
    }
  }

  async function resend() {
    if (!email.trim()) return;
    setError("");
    setResending(true);
    try {
      await authApi.resendVerification(email.trim());
      setMessage("Mã xác thực mới đã được gửi. Hãy kiểm tra hộp thư hoặc Mailpit.");
    } finally {
      setResending(false);
    }
  }

  return (
    <AuthFormShell title="Xác thực tài khoản" description={message}>
      <span className="mb-6 grid size-14 place-items-center rounded-2xl bg-brand-50 text-brand-600"><MailCheck /></span>
      {success ? (
        <Link to="/login"><Button className="w-full" size="lg">Đi tới đăng nhập</Button></Link>
      ) : (
        <form className="space-y-4" onSubmit={verify}>
          <Input label="Email đăng ký" type="email" required value={email} onChange={(event) => setEmail(event.target.value)} />
          <Input
            className="text-center text-2xl font-bold tracking-[0.5em]"
            label="Mã xác thực"
            inputMode="numeric"
            maxLength={6}
            pattern="\d{6}"
            placeholder="000000"
            required
            value={code}
            onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, 6))}
          />
          {error && <p className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
          <Button className="w-full" disabled={code.length !== 6} loading={verifying} size="lg" type="submit">Xác nhận mã</Button>
          <Button className="w-full" loading={resending} onClick={resend} type="button" variant="outline">Gửi lại mã</Button>
        </form>
      )}
    </AuthFormShell>
  );
}
