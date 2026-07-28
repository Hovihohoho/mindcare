import { zodResolver } from "@hookform/resolvers/zod";
import { LockKeyhole } from "lucide-react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { z } from "zod";
import { Button, Input } from "@/shared";
import { AuthFormShell } from "../components/AuthFormShell";

const schema = z.object({ password: z.string().min(8, "Mật khẩu cần ít nhất 8 ký tự"), confirmPassword: z.string() }).refine((data) => data.password === data.confirmPassword, { path: ["confirmPassword"], message: "Mật khẩu không khớp" });
type ResetForm = z.infer<typeof schema>;

export function ResetPasswordPage() {
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<ResetForm>({ resolver: zodResolver(schema) });
  return (
    <AuthFormShell title="Tạo mật khẩu mới" description="Mật khẩu mới nên khác với những mật khẩu bạn đã sử dụng trước đây.">
      <form className="space-y-5" onSubmit={handleSubmit(async () => { await Promise.resolve(); navigate("/login"); })}>
        <Input label="Mật khẩu" type="password" leading={<LockKeyhole className="size-4" />} error={errors.password?.message} {...register("password")} />
        <Input label="Xác nhận mật khẩu" type="password" leading={<LockKeyhole className="size-4" />} error={errors.confirmPassword?.message} {...register("confirmPassword")} />
        <Button className="w-full" size="lg" loading={isSubmitting}>Cập nhật mật khẩu</Button>
      </form>
    </AuthFormShell>
  );
}
