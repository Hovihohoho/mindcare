import { Suspense } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "react-router-dom";
import { queryClient } from "./providers/queryClient";
import { appRouter } from "./router";
import { ToastHost } from "@/shared";
import { AppErrorBoundary } from "./components/AppErrorBoundary";
import { RealtimeProvider } from "@/shared/realtime/RealtimeProvider";

export function App() {
  return (
    <AppErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <RealtimeProvider>
          <Suspense fallback={<div className="grid min-h-screen place-items-center text-sm text-muted">Đang tải...</div>}>
            <RouterProvider router={appRouter} />
          </Suspense>
          <ToastHost />
        </RealtimeProvider>
      </QueryClientProvider>
    </AppErrorBoundary>
  );
}
