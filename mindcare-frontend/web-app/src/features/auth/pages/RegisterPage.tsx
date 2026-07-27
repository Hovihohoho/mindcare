import { zodResolver } from "@hookform/resolvers/zod";
import { LockKeyhole, Mail, UserRound } from "lucide-react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { z } from "zod";
import { Button, Input } from "@/shared";
import { authApi } from "../api/auth.api";
import { AuthFormShell } from "../components/AuthFormShell";

const schema = z.object({
  fullName: z.string().min(2, "Vui lòng nhập họ tên"),
  email: z.email("Email không hợp lệ"),
  password: z.string().min(6, "Mật khẩu cần ít nhất 6 ký tự"),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, { path: ["confirmPassword"], message: "Mật khẩu không khớp" });
type RegisterForm = z.infer<typeof schema>;

export function RegisterPage() {
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<RegisterForm>({ resolver: zodResolver(schema) });
  const submit = async (form: RegisterForm) => {
    const payload = { fullName: form.fullName, email: form.email, password: form.password };
    await authApi.register(payload);
    navigate(`/verify-email?email=${encodeURIComponent(payload.email)}`);
  };

  return (
    <AuthFormShell title="Bắt đầu hành trình mới" description="Tham gia cộng đồng MindCare để nhận được sự hỗ trợ tốt nhất." footer={<>Đã có tài khoản? <Link className="font-bold text-brand-600" to="/login">Đăng nhập ngay</Link></>}>
      <form className="space-y-4" onSubmit={handleSubmit(submit)}>
        <Input label="Họ và tên" placeholder="Nguyễn Văn A" leading={<UserRound className="size-4" />} error={errors.fullName?.message} {...register("fullName")} />
        <Input label="Email" type="email" placeholder="example@email.com" leading={<Mail className="size-4" />} error={errors.email?.message} {...register("email")} />
        <Input label="Mật khẩu" type="password" leading={<LockKeyhole className="size-4" />} error={errors.password?.message} {...register("password")} />
        <Input label="Xác nhận mật khẩu" type="password" leading={<LockKeyhole className="size-4" />} error={errors.confirmPassword?.message} {...register("confirmPassword")} />
        <Button className="w-full" size="lg" loading={isSubmitting}>Tạo tài khoản</Button>
      </form>
    </AuthFormShell>
  );
}
