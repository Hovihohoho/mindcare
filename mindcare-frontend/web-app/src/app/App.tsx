import { QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "react-router-dom";
import { queryClient } from "./providers/queryClient";
import { appRouter } from "./router";
import { ToastHost } from "@/shared";
import { AppErrorBoundary } from "./components/AppErrorBoundary";

export function App() {
  return (
    <AppErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={appRouter} />
        <ToastHost />
      </QueryClientProvider>
    </AppErrorBoundary>
  );
}
