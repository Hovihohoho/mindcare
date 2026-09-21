import { useState } from "react";
import { MailCheck } from "lucide-react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button, Input } from "@/shared";
import { authApi } from "../api/auth.api";
import { AuthFormShell } from "../components/AuthFormShell";

function errorMessage(error: unknown) {
  return (error as { response?: { data?: { message?: string } } })
    .response?.data?.message ?? "Không thể xác thực. Vui lòng thử lại.";
}

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ text: string; error: boolean } | null>(null);
  const email = params.get("email") ?? "";

  const submit = async () => {
    setLoading(true);
    setMessage(null);
    try {
      if (!email) {
        setMessage({ text: "Không tìm thấy email. Vui lòng đăng ký lại.", error: true });
        return;
      }
      await authApi.verifyEmail(email, code);
      navigate("/login", { replace: true });
    } catch (error) {
      setMessage({ text: errorMessage(error), error: true });
    } finally {
      setLoading(false);
    }
  };

  const resend = async () => {
    if (!email) {
      setMessage({ text: "Không tìm thấy email. Vui lòng đăng ký lại.", error: true });
      return;
    }
    setLoading(true);
    setMessage(null);
    try {
      await authApi.resendVerification(email);
      setMessage({ text: "Mã xác thực mới đã được gửi. Vui lòng kiểm tra cả thư rác.", error: false });
    } catch (error) {
      setMessage({ text: errorMessage(error), error: true });
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthFormShell title="Xác thực tài khoản" description={`Nhập mã gồm 6 chữ số đã gửi đến ${email || "email của bạn"}.`}>
      <span className="mb-6 grid size-14 place-items-center rounded-2xl bg-brand-50 text-brand-600"><MailCheck /></span>
      {message && <p role="status" className={`mb-4 rounded-xl p-3 text-sm ${message.error ? "bg-rose-50 text-rose-700" : "bg-emerald-50 text-emerald-700"}`}>{message.text}</p>}
      <Input
        label="Mã xác thực"
        inputMode="numeric"
        autoComplete="one-time-code"
        value={code}
        onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, 6))}
        maxLength={6}
        placeholder="000000"
        className="text-center text-xl tracking-[.45em]"
      />
      <Button className="mt-5 w-full" size="lg" onClick={submit} loading={loading} disabled={code.length !== 6}>Xác thực</Button>
      <button type="button" disabled={loading} className="mt-4 w-full text-sm font-semibold text-brand-600 disabled:opacity-50" onClick={resend}>Gửi lại mã</button>
    </AuthFormShell>
  );
}
