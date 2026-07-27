import axios from "axios";
import { tokenStorage } from "@/shared/lib/storage";

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
    if (error.response?.status === 401) tokenStorage.clear();
    return Promise.reject(error);
  },
);
