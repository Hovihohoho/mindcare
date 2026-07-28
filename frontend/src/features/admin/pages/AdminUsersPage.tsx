import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Button, Card, Input, PageHeader } from "@/shared";
import { adminApi } from "../api/admin.api";
import type { AdminUser } from "../types/admin.types";

export function AdminUsersPage() {
  const queryClient = useQueryClient();
  const users = useQuery({ queryKey: ["admin", "users"], queryFn: adminApi.users });
  const roles = useQuery({ queryKey: ["admin", "roles"], queryFn: adminApi.roles });
  const [editing, setEditing] = useState<AdminUser | null>(null);
  const [creating, setCreating] = useState(false);
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
  const remove = useMutation({ mutationFn: adminApi.deleteUser, onSuccess: refresh });

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    if (editing) {
      await adminApi.updateUser(editing.id, { fullName: String(values.fullName), email: String(values.email), role: String(values.role), active: values.active === "on" });
    } else {
      await adminApi.createExpert({ fullName: String(values.fullName), email: String(values.email), password: String(values.password) });
    }
    setEditing(null);
    setCreating(false);
    await refresh();
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Quản lý tài khoản" description="Quản lý người dùng và cấp tài khoản chuyên gia." actions={<Button onClick={() => { setEditing(null); setCreating(true); }}>Tạo chuyên gia</Button>} />
      {(creating || editing) && <Card className="p-6"><form className="grid gap-4 md:grid-cols-2" onSubmit={submit}><Input name="fullName" label="Họ tên" required defaultValue={editing?.fullName} /><Input name="email" label="Email" type="email" required defaultValue={editing?.email} />{editing ? <><label className="text-sm font-semibold">Vai trò<select className="mt-2 h-11 w-full rounded-xl border border-line px-3" name="role" defaultValue={editing.role}>{roles.data?.map((role) => <option key={role.id} value={role.name}>{role.name}</option>)}</select></label><label className="flex items-center gap-2 self-end pb-3"><input name="active" type="checkbox" defaultChecked={editing.active} />Đang hoạt động</label></> : <Input name="password" label="Mật khẩu tạm" type="password" minLength={8} required />}<div className="flex gap-3 md:col-span-2"><Button>Lưu</Button><Button type="button" variant="outline" onClick={() => { setCreating(false); setEditing(null); }}>Hủy</Button></div></form></Card>}
      <Card className="overflow-x-auto">
        <table className="w-full min-w-[800px] text-left text-sm"><thead className="bg-slate-100"><tr><th className="p-4">Họ tên</th><th>Email</th><th>Vai trò</th><th>Trạng thái</th><th>Xác thực</th><th>Thao tác</th></tr></thead><tbody>{users.data?.map((user) => <tr className="border-t border-line" key={user.id}><td className="p-4 font-semibold">{user.fullName}</td><td>{user.email}</td><td>{user.role}</td><td>{user.active ? "Hoạt động" : "Đã khóa"}</td><td>{user.emailVerified ? "Đã xác thực" : "Chờ xác thực"}</td><td><div className="flex gap-2"><Button size="sm" variant="outline" onClick={() => { setCreating(false); setEditing(user); }}>Sửa</Button><Button size="sm" variant="ghost" loading={remove.isPending} onClick={() => window.confirm("Xóa tài khoản này?") && remove.mutate(user.id)}>Xóa</Button></div></td></tr>)}</tbody></table>
        {users.isError && <p className="p-5 text-rose-700">Không tải được tài khoản.</p>}
      </Card>
    </div>
  );
}
