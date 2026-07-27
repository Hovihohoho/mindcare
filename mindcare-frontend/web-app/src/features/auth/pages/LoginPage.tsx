import { zodResolver } from "@hookform/resolvers/zod";
import { LockKeyhole, Mail } from "lucide-react";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { z } from "zod";
import { Button, Input } from "@/shared";
import { AuthFormShell } from "../components/AuthFormShell";
import { useLogin } from "../hooks/useLogin";

const schema = z.object({
  email: z.email("Email không hợp lệ"),
  password: z.string().min(6, "Mật khẩu cần ít nhất 6 ký tự"),
});
type LoginForm = z.infer<typeof schema>;

export function LoginPage() {
  const login = useLogin();
  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({ resolver: zodResolver(schema) });

  return (
    <AuthFormShell
      title="Chào mừng bạn trở lại"
      description="Đăng nhập để tiếp tục chăm sóc sức khỏe tinh thần của bạn."
      footer={<>Chưa có tài khoản? <Link className="font-bold text-brand-600" to="/register">Đăng ký ngay</Link></>}
    >
      <form className="space-y-5" onSubmit={handleSubmit((values) => login.mutate(values))}>
        <Input label="Email" type="email" placeholder="name@example.com" leading={<Mail className="size-4" />} error={errors.email?.message} {...register("email")} />
        <Input label="Mật khẩu" type="password" placeholder="••••••••" leading={<LockKeyhole className="size-4" />} error={errors.password?.message} {...register("password")} />
        <div className="flex items-center justify-between gap-4 text-sm text-slate-500">
          <label className="flex items-center gap-2"><input className="size-4 rounded border-line" type="checkbox" />Ghi nhớ đăng nhập</label>
          <Link className="font-semibold text-brand-700" to="/forgot-password">Quên mật khẩu?</Link>
        </div>
        {login.isError && <p className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">Không thể đăng nhập. Vui lòng kiểm tra lại thông tin.</p>}
        <Button className="w-full" size="lg" loading={login.isPending}>Đăng nhập</Button>
        <div className="flex items-center gap-4 py-2 text-xs text-slate-400"><span className="h-px flex-1 bg-line" />Hoặc đăng nhập bằng<span className="h-px flex-1 bg-line" /></div>
        <button className="flex h-12 w-full items-center justify-center gap-3 rounded-full border border-line font-semibold text-slate-700"><span className="font-bold text-blue-600">G</span>Google</button>
      </form>
    </AuthFormShell>
  );
}
