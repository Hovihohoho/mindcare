import { useMutation, useQuery } from "@tanstack/react-query";
import { authApi } from "@/features/auth";
import { Button, Card, Input, Loading, PageHeader } from "@/shared";
import { tokenStorage } from "@/shared/lib/storage";
import { ReminderSettings } from "../components/ReminderSettings";

export function SettingsPage() {
  const sessions = useQuery({ queryKey: ["auth-sessions"], queryFn: authApi.sessions });
  const password = useMutation({
    mutationFn: ({ current, next }: { current: string; next: string }) => authApi.changePassword(current, next),
    onSuccess: () => {
      tokenStorage.clear();
      window.location.assign("/login?reason=password-changed");
    },
  });
  const revoke = useMutation({ mutationFn: authApi.revokeSession, onSuccess: () => sessions.refetch() });
  const deactivate = useMutation({
    mutationFn: authApi.deactivate,
    onSuccess: () => { tokenStorage.clear(); window.location.assign("/login"); },
  });

  function changePassword(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    password.mutate({ current: String(data.get("current")), next: String(data.get("next")) });
  }

  return (
    <div className="space-y-8">
      <ReminderSettings />
      <PageHeader title="Bảo mật tài khoản" description="Mật khẩu, phiên đăng nhập và trạng thái tài khoản." />
      <Card className="p-7">
        <h2 className="text-xl font-bold">Đổi mật khẩu</h2>
        <form className="mt-5 grid max-w-xl gap-4" onSubmit={changePassword}>
          <Input label="Mật khẩu hiện tại" name="current" required type="password" />
          <Input label="Mật khẩu mới" minLength={8} name="next" required type="password" />
          {password.isSuccess && <p className="text-sm text-emerald-700">Đã đổi mật khẩu. Các phiên cũ đã bị đăng xuất.</p>}
          {password.isError && <p className="text-sm text-rose-600">Không thể đổi mật khẩu.</p>}
          <Button className="w-fit" loading={password.isPending}>Đổi mật khẩu</Button>
        </form>
      </Card>
      <Card className="p-7">
        <h2 className="text-xl font-bold">Phiên đăng nhập</h2>
        {sessions.isLoading ? <Loading /> : <div className="mt-4 divide-y divide-line">{sessions.data?.map((item) => <div className="flex flex-wrap items-center justify-between gap-4 py-4" key={item.id}><div><p className="font-semibold">{item.userAgent || "Thiết bị không xác định"}</p><p className="text-sm text-muted">{item.ipAddress} · hoạt động {new Date(item.lastSeenAt).toLocaleString("vi-VN")}</p></div><Button disabled={item.revoked} onClick={() => revoke.mutate(item.id)} variant="outline">{item.revoked ? "Đã đăng xuất" : "Đăng xuất phiên"}</Button></div>)}</div>}
      </Card>
      <Card className="border-rose-200 p-7">
        <h2 className="text-xl font-bold text-rose-700">Vô hiệu hóa tài khoản</h2>
        <p className="mt-2 text-sm text-muted">Bạn sẽ bị đăng xuất và không thể đăng nhập cho tới khi admin mở khóa.</p>
        <Button className="mt-5" loading={deactivate.isPending} onClick={() => { if (confirm("Bạn chắc chắn muốn vô hiệu hóa tài khoản?")) deactivate.mutate(); }} variant="outline">Vô hiệu hóa</Button>
      </Card>
    </div>
  );
}
