import { ArrowRight, CalendarDays, LockKeyhole, QrCode, ShieldCheck, Star, UserRound, Video, WalletCards } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Avatar, Button, Card, Input, Textarea } from "@/shared";
import { PaymentMethodCard } from "../components/PaymentMethodCard";

export function PaymentPage() {
  const navigate = useNavigate();
  const [method, setMethod] = useState("MOMO");
  const [paid, setPaid] = useState(false);
  if (paid) return <div className="mx-auto max-w-xl py-20 text-center"><span className="mx-auto grid size-20 place-items-center rounded-full bg-emerald-100 text-4xl">✓</span><h1 className="mt-6 text-3xl font-bold">Đặt lịch thành công</h1><p className="mt-3 text-muted">Thông tin cuộc hẹn đã được gửi đến email của bạn.</p><Button className="mt-7" onClick={() => navigate("/")}>Về trang chủ</Button></div>;
  return (
    <div>
      <p className="text-sm text-slate-500">Chuyên gia <span className="mx-2">›</span> Hồ sơ chuyên gia <span className="mx-2">›</span><b className="text-brand-700">Xác nhận & Thanh toán</b></p>
      <h1 className="mt-8 text-4xl font-bold text-brand-700">Xác nhận và Thanh toán</h1>
      <p className="mt-3 text-slate-500">Vui lòng kiểm tra kỹ thông tin trước khi hoàn tất đặt lịch.</p>
      <div className="mt-14 grid items-start gap-8 lg:grid-cols-[1fr_380px]">
        <div className="space-y-7">
          <Card className="p-7">
            <h2 className="flex items-center gap-3 text-2xl font-semibold"><CalendarDays className="text-brand-700" />Tóm tắt lịch hẹn</h2>
            <div className="mt-6 flex items-center gap-5 rounded-xl bg-slate-50 p-4"><Avatar className="size-20" fallback="NA" /><div><p className="text-xl font-semibold">TS. Nguyễn Văn A</p><p className="mt-1 text-sm font-semibold text-brand-700">PSYCHOLOGIST</p><p className="mt-2"><Star className="mr-1 inline size-5 fill-amber-400 text-amber-400" /><b>4.9</b> <span className="text-muted">(120 đánh giá)</span></p></div></div>
            <div className="mt-6 grid gap-5 sm:grid-cols-2"><div><p className="text-sm text-muted">Hình thức</p><p className="mt-2 flex items-center gap-2 font-semibold"><Video className="size-5 text-emerald-700" />Tư vấn Online</p></div><div><p className="text-sm text-muted">Ngày & Giờ</p><p className="mt-2 flex items-center gap-2 font-semibold"><CalendarDays className="size-5 text-brand-700" />11/11/2024 • 09:30</p></div></div>
            <p className="mt-6 rounded-lg border border-sky-200 bg-sky-50 p-3 text-xs text-brand-700">ⓘ Link tham gia Google Meet sẽ được gửi vào email của bạn ngay sau khi thanh toán thành công.</p>
          </Card>
          <Card className="p-7">
            <h2 className="flex items-center gap-3 text-2xl font-semibold"><UserRound className="text-brand-700" />Thông tin cá nhân</h2>
            <div className="mt-6 grid gap-5 sm:grid-cols-2"><Input label="Họ và tên" placeholder="Nhập họ và tên" /><Input label="Số điện thoại" placeholder="0xxx xxx xxx" /><div className="sm:col-span-2"><Input label="Email" type="email" placeholder="email@example.com" /></div><div className="sm:col-span-2"><Textarea label="Lưu ý cho chuyên gia (Không bắt buộc)" placeholder="Chia sẻ sơ qua vấn đề bạn đang gặp phải hoặc các mong muốn trong buổi tư vấn..." /></div></div>
          </Card>
          <Card className="p-7"><h2 className="flex items-center gap-3 text-2xl font-semibold"><WalletCards className="text-brand-700" />Phương thức thanh toán</h2><div className="mt-6 grid gap-4 sm:grid-cols-2"><PaymentMethodCard active={method === "MOMO"} title="Ví MoMo" description="Thanh toán qua ví điện tử" icon={<span className="font-bold text-pink-600">MoMo</span>} onClick={() => setMethod("MOMO")} /><PaymentMethodCard active={method === "QR"} title="Chuyển khoản QR" description="Quét mã qua ngân hàng" icon={<QrCode />} onClick={() => setMethod("QR")} /></div></Card>
        </div>
        <Card className="overflow-hidden lg:sticky lg:top-28">
          <h2 className="bg-brand-700 px-7 py-6 text-2xl font-semibold text-white">Thông tin đơn hàng</h2>
          <div className="p-7"><div className="flex justify-between"><span className="text-muted">Phí tư vấn (60 phút)</span><b>150.000đ</b></div><div className="mt-6 flex justify-between"><span className="text-muted">Phí nền tảng</span><b>50.000đ</b></div><div className="mt-7 flex items-end justify-between border-t border-line pt-7"><b className="text-xl">Tổng thanh toán</b><b className="text-3xl text-brand-700">200.000đ</b></div><Button className="mt-7 w-full" size="lg" onClick={() => setPaid(true)}>Thanh toán ngay <ArrowRight className="size-5" /></Button><p className="mt-8 flex gap-2 text-xs text-muted"><ShieldCheck className="size-5 shrink-0 text-emerald-700" />Mọi giao dịch đều được mã hóa và bảo mật 100%. Thông tin cá nhân của bạn được cam kết tuyệt mật.</p><p className="mt-5 flex items-center gap-2 rounded-lg border border-line p-3 text-xs text-muted"><LockKeyhole className="size-4" />Chứng nhận bởi SafePay Compliance</p></div>
        </Card>
      </div>
    </div>
  );
}
