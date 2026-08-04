import { Component, type ErrorInfo, type ReactNode } from "react";
import { Button, Logo } from "@/shared";

interface State { failed: boolean }

export class AppErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { failed: false };

  static getDerivedStateFromError(): State {
    return { failed: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("Unhandled application error", error, info.componentStack);
  }

  render() {
    if (!this.state.failed) return this.props.children;
    return (
      <main className="grid min-h-screen place-items-center bg-slate-50 px-5 text-center">
        <div className="max-w-lg">
          <Logo />
          <p className="mt-8 text-7xl font-black text-brand-100">500</p>
          <h1 className="mt-3 text-3xl font-extrabold">Ứng dụng gặp sự cố</h1>
          <p className="mt-3 text-muted">Hãy tải lại trang. Nếu lỗi tiếp tục xảy ra, vui lòng thử lại sau.</p>
          <Button className="mt-7" onClick={() => window.location.reload()}>Tải lại trang</Button>
        </div>
      </main>
    );
  }
}
