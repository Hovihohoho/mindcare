import { Link } from "react-router-dom";
import { cn } from "@/shared";
import { homeServices } from "../constants/home.constants";

export function ServicesSection() {
  return (
    <section className="bg-gray-100 px-5 py-16 md:px-10">
      <div className="mx-auto max-w-[896px]">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-3xl font-bold leading-10 text-slate-800">Giải pháp toàn diện cho tâm trí của bạn</h2>
          <p className="mt-4 leading-6 text-slate-500">
            Chúng tôi cung cấp các công cụ và nguồn lực cần thiết để bạn hiểu rõ bản thân và cải thiện chất lượng cuộc sống tinh thần.
          </p>
        </div>
        <div className="mt-12 grid gap-6 md:grid-cols-2">
          {homeServices.map(({ icon: Icon, ...service }) => (
            <Link className="group" key={service.title} to={service.to}>
              <article className="h-full min-h-[240px] rounded-2xl border border-slate-200 bg-white p-8 shadow-[0_10px_30px_-5px_rgba(95,168,211,0.08)] transition group-hover:-translate-y-1 group-hover:shadow-lg">
                <span className={cn("grid size-14 place-items-center rounded-xl text-slate-800", service.tone)}><Icon className="size-6" /></span>
                <h3 className="mt-6 text-2xl font-semibold leading-8 text-slate-800">{service.title}</h3>
                <p className="mt-3 leading-6 text-slate-500">{service.description}</p>
              </article>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}
