import { Card, PageHeader } from "@/shared";

export function PaymentPage() {
  return (
    <div className="space-y-8">
      <PageHeader title="Thanh toán" description="Thông tin thanh toán từ Booking Service." />
      <Card className="p-10 text-center text-muted">
        Payment provider và API checkout chưa được triển khai. Ở môi trường local, booking được cấu hình không yêu cầu thanh toán.
      </Card>
    </div>
  );
}
