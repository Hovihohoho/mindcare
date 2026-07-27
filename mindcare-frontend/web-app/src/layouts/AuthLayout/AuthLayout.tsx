import { Outlet } from "react-router-dom";
import { Logo } from "@/shared";

export function AuthLayout() {
  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#126d77] p-4 md:p-10 lg:p-16">
      <div className="absolute -left-20 -top-10 size-[360px] rounded-full bg-white/5" />
      <div className="absolute -bottom-36 right-[-40px] size-[420px] rounded-full bg-white/5" />
      <div className="relative grid w-full max-w-[1300px] overflow-hidden rounded-[30px] bg-white shadow-2xl lg:min-h-[850px] lg:grid-cols-2">
        <section className="relative hidden overflow-hidden lg:block">
          <img className="absolute inset-0 size-full object-cover" src="/assets/mindcare-wellness-illustration.png" alt="" />
          <div className="absolute inset-x-0 top-0 h-40 bg-gradient-to-b from-white/55 to-transparent" />
          <Logo className="absolute left-10 top-8 z-10" />
        </section>
        <section className="flex items-center justify-center px-6 py-12 md:px-14 lg:px-20">
          <div className="w-full max-w-[470px]">
          <Logo className="mb-10 lg:hidden" />
          <Outlet />
        </div>
        </section>
      </div>
    </main>
  );
}
