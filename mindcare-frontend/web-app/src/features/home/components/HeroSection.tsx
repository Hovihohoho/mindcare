/* Hallmark · pre-emit critique: P5 H4 E5 S4 R5 V4 */
import { ArrowRight, Bot, ShieldCheck } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/shared";

export function HeroSection() {
  return (
    <section className="relative overflow-hidden bg-[radial-gradient(circle_at_70%_30%,#eff6ff,transparent_60%)] py-14 lg:py-16">
      <div className="page-container grid min-h-[564px] items-center gap-10 lg:grid-cols-2 lg:gap-8">
        <div className="flex flex-col items-start gap-8">
          <span className="inline-flex items-center gap-2 rounded-full bg-emerald-200 px-4 py-1.5 text-sm font-medium text-teal-800">
            <ShieldCheck className="size-4" /> Đồng hành riêng tư mỗi ngày
          </span>
          <h1 className="max-w-[620px] text-4xl font-bold leading-[1.2] text-slate-800 md:text-5xl">
            Chăm sóc sức khỏe <span className="text-brand-700">tinh thần</span> mỗi ngày.
          </h1>
          <p className="max-w-[590px] text-base leading-7 text-slate-500 md:text-lg">
            Theo dõi cảm xúc, thực hiện bài đánh giá tâm lý và nhận gợi ý từ AI trong một không gian riêng tư, dễ sử dụng.
          </p>
          <div className="flex w-full max-w-[300px] flex-col gap-4">
            <Link to="/assessments">
              <Button className="w-full" size="lg">Bắt đầu đánh giá <ArrowRight className="size-5" /></Button>
            </Link>
            <Link to="/ai-chat">
              <Button className="w-full text-brand-700" size="lg" variant="outline" leftIcon={<Bot className="size-5" />}>Trò chuyện với AI</Button>
            </Link>
          </div>
        </div>
        <div className="relative mx-auto w-full max-w-[640px]">
          <div className="absolute -right-6 -top-10 size-64 rounded-full bg-sky-200/30 blur-3xl" />
          <div className="absolute -bottom-8 -left-8 size-64 rounded-full bg-emerald-200/30 blur-3xl" />
          <img
            className="relative aspect-[640/429] w-full rounded-[4px] object-cover shadow-[0_25px_25px_rgba(0,0,0,0.15)]"
            src="/assets/mindcare-wellness-illustration.png"
            alt="Không gian chăm sóc sức khỏe tinh thần"
          />
        </div>
      </div>
    </section>
  );
}
