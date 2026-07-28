import { useState } from "react";
import { MailCheck } from "lucide-react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button, Input } from "@/shared";
import { authApi } from "../api/auth.api";
import { AuthFormShell } from "../components/AuthFormShell";

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);
  const email = params.get("email") ?? "";

  const submit = async () => {
    setLoading(true);
    try { await authApi.verifyEmail(code); navigate("/login"); } finally { setLoading(false); }
  };

  return (
    <AuthFormShell title="Xác thực tài khoản" description="Vui lòng nhập mã xác thực gồm 6 chữ số đã được gửi đến email của bạn.">
      <span className="mb-6 grid size-14 place-items-center rounded-2xl bg-brand-50 text-brand-600"><MailCheck /></span>
      <Input label="Mã xác thực" value={code} onChange={(event) => setCode(event.target.value)} maxLength={6} placeholder="000000" className="text-center text-xl tracking-[.45em]" />
      <Button className="mt-5 w-full" size="lg" onClick={submit} loading={loading} disabled={code.length < 6}>Xác thực</Button>
      <button className="mt-4 w-full text-sm font-semibold text-brand-600" onClick={() => authApi.resendVerification(email)}>Gửi lại mã</button>
    </AuthFormShell>
  );
}
