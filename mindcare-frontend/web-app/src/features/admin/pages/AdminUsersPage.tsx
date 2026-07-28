import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Search, UserPlus } from "lucide-react";
import { useState } from "react";
import { Badge, Button, Card, Input, PageHeader } from "@/shared";
import { adminApi } from "../api/admin.api";
import type { AdminUser } from "../types/admin.types";

export function AdminUsersPage() {
  const client = useQueryClient();
  const users = useQuery({ queryKey: ["admin", "users"], queryFn: adminApi.users });
  const roles = useQuery({ queryKey: ["admin", "roles"], queryFn: adminApi.roles });
  const [editing, setEditing] = useState<AdminUser | null>(null);
  const [creating, setCreating] = useState(false);
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const refresh = () => client.invalidateQueries({ queryKey: ["admin", "users"] });
  const remove = useMutation({ mutationFn: adminApi.deleteUser, onSuccess: refresh });
  const filtered = users.data?.filter((user) => `${user.fullName} ${user.email} ${user.role}`.toLowerCase().includes(search.toLowerCase()));

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    const values = Object.fromEntries(new FormData(event.currentTarget));
    try {
      if (editing) await adminApi.updateUser(editing.id, { fullName: String(values.fullName), email: String(values.email), role: String(values.role), active: values.active === "on" });
      else await adminApi.createExpert({ fullName: String(values.fullName), email: String(values.email), password: String(values.password) });
      setEditing(null);
      setCreating(false);
      await refresh();
    } catch {
      setError("Không thể lưu tài khoản. Email có thể đã được sử dụng.");
    }
  }

  return <div className="space-y-6">
    <PageHeader title="Quản lý tài khoản" description="Tìm kiếm, phân quyền, khóa tài khoản và tạo chuyên gia." actions={<Button leftIcon={<UserPlus className="size-4" />} onClick={() => { setEditing(null); setCreating(true); }}>Tạo chuyên gia</Button>} />
    <div className="max-w-md"><Input leading={<Search className="size-4" />} placeholder="Tìm theo tên, email hoặc vai trò..." value={search} onChange={(event) => setSearch(event.target.value)} /></div>
    {(creating || editing) && <Card className="p-6"><h2 className="mb-5 text-lg font-bold">{editing ? "Chỉnh sửa tài khoản" : "Tạo tài khoản chuyên gia"}</h2><form className="grid gap-4 md:grid-cols-2" onSubmit={submit}><Input defaultValue={editing?.fullName} label="Họ tên" name="fullName" required /><Input defaultValue={editing?.email} label="Email" name="email" required type="email" />{editing ? <><label className="text-sm font-semibold">Vai trò<select className="mt-2 h-12 w-full rounded-xl border border-line px-3" defaultValue={editing.role} name="role">{roles.data?.map((role) => <option key={role.id} value={role.name}>{role.name.replace("ROLE_", "")}</option>)}</select></label><label className="flex items-center gap-2 self-end pb-3"><input defaultChecked={editing.active} name="active" type="checkbox" />Tài khoản hoạt động</label></> : <Input label="Mật khẩu tạm" minLength={8} name="password" required type="password" />}{error && <p className="text-sm text-rose-600 md:col-span-2">{error}</p>}<div className="flex gap-3 md:col-span-2"><Button>Lưu tài khoản</Button><Button onClick={() => { setCreating(false); setEditing(null); }} type="button" variant="outline">Hủy</Button></div></form></Card>}
    <Card className="overflow-x-auto"><table className="w-full min-w-[850px] text-left text-sm"><thead className="bg-slate-100 text-slate-600"><tr><th className="p-4">Người dùng</th><th>Vai trò</th><th>Trạng thái</th><th>Email</th><th className="text-right">Thao tác</th></tr></thead><tbody>{filtered?.map((user) => <tr className="border-t border-line" key={user.id}><td className="p-4"><b>{user.fullName}</b><p className="mt-1 text-xs text-muted">{user.email}</p></td><td><Badge>{user.role.replace("ROLE_", "")}</Badge></td><td><Badge tone={user.active ? "success" : "warning"}>{user.active ? "Hoạt động" : "Đã khóa"}</Badge></td><td>{user.emailVerified ? "Đã xác thực" : "Chờ xác thực"}</td><td><div className="flex justify-end gap-2"><Button size="sm" variant="outline" onClick={() => { setCreating(false); setEditing(user); }}>Sửa</Button><Button size="sm" variant="ghost" loading={remove.isPending} onClick={() => window.confirm(`Xóa tài khoản ${user.email}?`) && remove.mutate(user.id)}>Xóa</Button></div></td></tr>)}</tbody></table>{users.isError && <p className="p-5 text-rose-700">Không tải được danh sách tài khoản.</p>}{!filtered?.length && !users.isLoading && <p className="p-8 text-center text-muted">Không tìm thấy tài khoản phù hợp.</p>}</Card>
  </div>;
}
