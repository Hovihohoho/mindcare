import { useEffect, useState } from "react";
import { MailCheck } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { Button } from "@/shared";
import { authApi } from "../api/auth.api";
import { AuthFormShell } from "../components/AuthFormShell";

type Status = "waiting" | "verifying" | "success" | "error";

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const token = params.get("token");
  const email = params.get("email") ?? "";
  const [status, setStatus] = useState<Status>(token ? "verifying" : "waiting");
  const [message, setMessage] = useState(
    token ? "Đang xác thực email…" : "Hãy mở email và nhấp vào liên kết xác thực MindCare.",
  );
  const [resending, setResending] = useState(false);

  useEffect(() => {
    if (!token) return;
    authApi.verifyEmail(token)
      .then(() => {
        setStatus("success");
        setMessage("Email đã được xác thực. Bạn có thể đăng nhập.");
      })
      .catch(() => {
        setStatus("error");
        setMessage("Liên kết không hợp lệ hoặc đã hết hạn.");
      });
  }, [token]);

  const resend = async () => {
    if (!email) return;
    setResending(true);
    try {
      await authApi.resendVerification(email);
      setMessage("Nếu tài khoản đang chờ xác thực, email mới đã được gửi.");
    } finally {
      setResending(false);
    }
  };

  return (
    <AuthFormShell title="Xác thực tài khoản" description={message}>
      <span className="mb-6 grid size-14 place-items-center rounded-2xl bg-brand-50 text-brand-600"><MailCheck /></span>
      {status === "verifying" && <p className="rounded-xl bg-sky-50 p-4 text-sm text-brand-700">Vui lòng chờ trong giây lát…</p>}
      {status === "error" && <p className="rounded-xl bg-rose-50 p-4 text-sm text-rose-700">{message}</p>}
      {status === "success" ? (
        <Link to="/login"><Button className="mt-5 w-full" size="lg">Đi tới đăng nhập</Button></Link>
      ) : (
        <Button className="mt-5 w-full" variant="outline" onClick={resend} loading={resending} disabled={!email}>
          Gửi lại email xác thực
        </Button>
      )}
    </AuthFormShell>
  );
}
