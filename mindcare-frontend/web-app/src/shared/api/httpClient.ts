import axios from "axios";
import { tokenStorage } from "@/shared/lib/storage";
import { notify } from "@/shared/lib/feedback";

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? "http://localhost:8079",
  headers: { "Content-Type": "application/json" },
  timeout: 12_000,
});

httpClient.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const message = error.response?.data?.message;
    const requestUrl = String(error.config?.url ?? "");
    const requestMethod = String(error.config?.method ?? "get").toLowerCase();
    const isSessionEndpoint = requestUrl === "/api/auth/me"
      || requestUrl === "/api/auth/logout"
      || requestUrl.startsWith("/api/auth/notifications");
    // Some older auth-service builds returned 403 when the bearer token was
    // expired/revoked. Treat that response as an expired session only on
    // endpoints that every authenticated role is allowed to use.
    const invalidMeSession = status === 400
      && requestMethod === "get"
      && requestUrl === "/api/auth/me";
    const isExpiredSession = status === 401
      || (status === 403 && isSessionEndpoint)
      || invalidMeSession;
    if (isExpiredSession && tokenStorage.get()) {
      tokenStorage.clear();
      const current = `${window.location.pathname}${window.location.search}`;
      if (requestUrl !== "/api/auth/logout" && !window.location.pathname.startsWith("/login")) {
        window.location.replace(`/login?reason=session-expired&returnTo=${encodeURIComponent(current)}`);
      }
    } else if (status === 403) {
      notify(message || "Bạn không có quyền thực hiện thao tác này.", "error");
    } else if (status === 429) {
      notify(message || "Bạn thao tác quá nhanh. Vui lòng thử lại sau.", "error");
    } else if (!error.response) {
      const timedOut = error.code === "ECONNABORTED" || error.code === "ETIMEDOUT";
      notify(timedOut
        ? "Máy chủ phản hồi quá lâu. Vui lòng chờ một chút rồi kiểm tra lại."
        : "Không thể kết nối đến máy chủ. Hãy kiểm tra hệ thống đang chạy.", "error");
    } else if (status >= 500) {
      notify(message || "Máy chủ đang gặp sự cố. Vui lòng thử lại sau.", "error");
    }
    return Promise.reject(error);
  },
);
