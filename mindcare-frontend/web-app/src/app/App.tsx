import { QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "react-router-dom";
import { queryClient } from "./providers/queryClient";
import { appRouter } from "./router";
import { RealtimeProvider } from "@/shared/realtime/RealtimeProvider";

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <RealtimeProvider>
        <RouterProvider router={appRouter} />
      </RealtimeProvider>
    </QueryClientProvider>
  );
}
