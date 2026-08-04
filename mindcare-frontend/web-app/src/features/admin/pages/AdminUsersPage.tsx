import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import type { UserRole } from "@/features/auth";
import { Button, Card, Input, Loading, PageHeader } from "@/shared";
import { adminApi } from "../api/admin.api";

function errorMessage(error: unknown) {
  return (error as { response?: { data?: { message?: string } } })
    .response?.data?.message ?? "Không thể thực hiện thao tác. Vui lòng thử lại.";
}

export function AdminUsersPage() {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [actionError, setActionError] = useState<string | null>(null);
  const users = useQuery({
    queryKey: ["admin-users", search, page],
    queryFn: () => adminApi.users(search, page),
  });
  const deleteUser = useMutation({
    mutationFn: adminApi.deleteUser,
    onMutate: () => setActionError(null),
    onSuccess: async () => {
      await users.refetch();
      if (page > 0 && users.data?.content.length === 1) setPage((value) => value - 1);
    },
    onError: (error) => setActionError(errorMessage(error)),
  });

  if (users.isLoading) return <Loading />;

  const remove = (id: string, name: string) => {
    if (window.confirm(`Bạn có chắc muốn xóa vĩnh viễn tài khoản “${name}”? Dữ liệu tài khoản sẽ không thể khôi phục.`)) {
      deleteUser.mutate(id);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Quản lý người dùng" description="Tìm kiếm, phân quyền, khóa hoặc xóa tài khoản." />
      <Input
        label="Tìm theo tên hoặc email"
        value={search}
        onChange={(event) => { setSearch(event.target.value); setPage(0); }}
      />
      {actionError && <p role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{actionError}</p>}
      <Card className="overflow-x-auto p-3">
        <table className="w-full text-sm">
          <thead><tr className="text-left"><th className="p-3">Người dùng</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
          <tbody>{users.data?.content.map((user) => (
            <tr className="border-t" key={user.id}>
              <td className="p-3"><b>{user.fullName}</b><br /><span className="text-muted">{user.email}</span></td>
              <td>
                <select
                  className="rounded-lg border p-2"
                  value={user.role}
                  onChange={(event) => adminApi.setRole(user.id, event.target.value as UserRole)
                    .then(() => users.refetch()).catch((error) => setActionError(errorMessage(error)))}
                >
                  <option>ROLE_USER</option><option>ROLE_EXPERT</option><option>ROLE_ADMIN</option>
                </select>
              </td>
              <td>{user.active ? "Hoạt động" : "Đã khóa"}</td>
              <td>
                <div className="flex gap-2">
                  <Button onClick={() => adminApi.setActive(user.id, !user.active)
                    .then(() => users.refetch()).catch((error) => setActionError(errorMessage(error)))} variant="outline">
                    {user.active ? "Khóa" : "Mở khóa"}
                  </Button>
                  <Button
                    disabled={deleteUser.isPending}
                    onClick={() => remove(user.id, user.fullName)}
                    variant="outline"
                    className="border-rose-300 text-rose-700 hover:bg-rose-50"
                  >
                    Xóa
                  </Button>
                </div>
              </td>
            </tr>
          ))}</tbody>
        </table>
      </Card>
      <div className="flex gap-3">
        <Button disabled={page === 0} onClick={() => setPage(page - 1)} variant="outline">Trước</Button>
        <Button disabled={page + 1 >= (users.data?.totalPages ?? 1)} onClick={() => setPage(page + 1)} variant="outline">Sau</Button>
      </div>
    </div>
  );
}
