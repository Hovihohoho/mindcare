import { Link } from "react-router-dom";
import { Button } from "@/shared";

export function TrustSection() {
  return (
    <section className="px-5 py-16 md:px-10">
      <div className="relative mx-auto max-w-[1152px] overflow-hidden rounded-3xl bg-blue-400 px-6 py-12 text-center text-white shadow-2xl md:px-16">
        <div className="absolute -left-16 -top-24 size-72 rounded-full border-[36px] border-white/10" />
        <div className="absolute -bottom-24 -right-16 size-64 rounded-full bg-white/10" />
        <div className="relative mx-auto max-w-[680px]">
          <h2 className="text-3xl font-bold leading-10">Sẵn sàng để bắt đầu hành trình thấu hiểu bản thân?</h2>
          <p className="mt-4 text-lg leading-7 text-white/90">Tạo tài khoản để lưu nhật ký cảm xúc, kết quả đánh giá và tiếp tục hành trình của bạn trên mọi thiết bị.</p>
          <Link className="mt-8 inline-block" to="/register">
            <Button className="bg-white px-16 text-brand-700 hover:bg-slate-50" size="lg">Tạo tài khoản miễn phí</Button>
          </Link>
        </div>
      </div>
    </section>
  );
}
