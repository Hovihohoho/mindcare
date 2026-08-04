import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, CalendarCheck2, LockKeyhole, Mail, UserRound } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { Avatar, Badge, Button, Card, Loading } from "@/shared";
import { clientRecordApi } from "../api/clientRecord.api";

export function ClientRecordPage() {
  const navigate = useNavigate();
  const { id = "" } = useParams();
  const profile = useQuery({
    queryKey: ["expert-client-profile", id],
    queryFn: () => clientRecordApi.byUser(id),
    enabled: Boolean(id),
  });

  if (profile.isLoading) return <Loading />;

  return (
    <div className="mx-auto max-w-[1100px] space-y-6">
      <Button
        variant="ghost"
        className="rounded-xl px-3"
        leftIcon={<ArrowLeft className="size-4" />}
        onClick={() => navigate(-1)}
      >
        Quay lại
      </Button>

      {profile.isError || !profile.data ? (
        <Card className="p-10 text-center">
          <h1 className="text-xl font-semibold text-slate-900">Không thể mở hồ sơ khách hàng</h1>
          <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-slate-600">
            Hồ sơ chỉ được hiển thị khi tài khoản này đã có booking với chuyên gia đang đăng nhập.
          </p>
        </Card>
      ) : (
        <>
          <Card className="rounded-2xl p-6 shadow-sm md:p-8">
            <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
              <Avatar className="size-24 border-4 border-sky-100 text-xl" fallback={profile.data.fullName} />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-3">
                  <h1 className="text-2xl font-semibold text-slate-900 md:text-3xl">{profile.data.fullName}</h1>
                  <Badge tone="success">Đã có lịch tư vấn</Badge>
                </div>
                <div className="mt-3 flex flex-wrap gap-x-6 gap-y-2 text-sm text-slate-600">
                  {profile.data.email && (
                    <span className="flex items-center gap-2">
                      <Mail className="size-4" />
                      {profile.data.email}
                    </span>
                  )}
                  <span className="flex items-center gap-2">
                    <UserRound className="size-4" />
                    Mã {profile.data.userId.slice(0, 8)}
                  </span>
                </div>
              </div>
            </div>
          </Card>

          <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
            <Card className="rounded-2xl p-6 md:p-8">
              <h2 className="flex items-center gap-3 text-lg font-semibold text-slate-900">
                <CalendarCheck2 className="size-5 text-brand-600" />
                Quan hệ tư vấn
              </h2>
              <dl className="mt-6 grid gap-5 sm:grid-cols-2">
                <div>
                  <dt className="text-sm text-slate-500">Trạng thái booking gần nhất</dt>
                  <dd className="mt-1 font-semibold text-slate-900">{profile.data.bookingStatus}</dd>
                </div>
                <div>
                  <dt className="text-sm text-slate-500">Mã booking</dt>
                  <dd className="mt-1 break-all text-sm font-medium text-slate-900">{profile.data.bookingId}</dd>
                </div>
              </dl>
            </Card>

            <aside className="rounded-2xl border border-sky-200 bg-sky-50 p-6">
              <LockKeyhole className="size-6 text-brand-700" />
              <h2 className="mt-4 font-semibold text-slate-900">Dữ liệu nhạy cảm được bảo vệ</h2>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                Kết quả đánh giá và nhật ký cảm xúc chưa hiển thị vì hệ thống chưa có API xác nhận đồng ý chia sẻ của người dùng.
              </p>
            </aside>
          </div>
        </>
      )}
    </div>
  );
}
