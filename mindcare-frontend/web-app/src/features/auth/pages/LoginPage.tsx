import { zodResolver } from "@hookform/resolvers/zod";
import { LockKeyhole, Mail } from "lucide-react";
import { useForm } from "react-hook-form";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { z } from "zod";
import { Button, Input } from "@/shared";
import { AuthFormShell } from "../components/AuthFormShell";
import { useLogin } from "../hooks/useLogin";

const schema = z.object({
  email: z.email("Email không hợp lệ"),
  password: z.string().min(8, "Mật khẩu cần ít nhất 8 ký tự"),
});
type LoginForm = z.infer<typeof schema>;

export function LoginPage() {
  const [params] = useSearchParams();
  const location = useLocation();
  const state = location.state as { from?: string } | null;
  const returnTo = params.get("returnTo") ?? state?.from;
  const login = useLogin(returnTo);
  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({
    resolver: zodResolver(schema),
  });
  const sessionExpired = params.get("reason") === "session-expired";
  const passwordChanged = params.get("reason") === "password-changed";
  const apiMessage = (login.error as { response?: { data?: { message?: string } } } | null)
    ?.response?.data?.message;

  return (
    <AuthFormShell
      title="Chào mừng bạn trở lại"
      description="Đăng nhập để tiếp tục sử dụng MindCare."
      footer={<>Chưa có tài khoản? <Link className="font-bold text-brand-600" to="/register">Đăng ký ngay</Link></>}
    >
      <form className="space-y-5" onSubmit={handleSubmit((values) => login.mutate(values))}>
        {sessionExpired && <p className="rounded-xl bg-amber-50 p-3 text-sm text-amber-800">Phiên đăng nhập đã hết hạn hoặc bị thu hồi. Vui lòng đăng nhập lại.</p>}
        {passwordChanged && <p className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-800">Đổi mật khẩu thành công. Vui lòng đăng nhập lại bằng mật khẩu mới.</p>}
        <Input label="Email" type="email" placeholder="name@example.com" leading={<Mail className="size-4" />} error={errors.email?.message} {...register("email")} />
        <Input label="Mật khẩu" type="password" placeholder="••••••••" leading={<LockKeyhole className="size-4" />} error={errors.password?.message} {...register("password")} />
        <div className="flex justify-end text-sm">
          <Link className="font-semibold text-brand-700" to="/forgot-password">Quên mật khẩu?</Link>
        </div>
        {login.isError && <p className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{apiMessage || "Không thể đăng nhập. Vui lòng kiểm tra lại thông tin."}</p>}
        <Button className="w-full" size="lg" loading={login.isPending}>Đăng nhập</Button>
      </form>
    </AuthFormShell>
  );
}
