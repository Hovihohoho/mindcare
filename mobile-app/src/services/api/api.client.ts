import { API_URL } from './api.config';

type ApiEnvelope<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
};

export type RequestOptions = {
  body?: unknown;
  headers?: Record<string, string>;
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  responseType?: 'envelope' | 'raw';
  timeout?: number;
  token?: string;
};

export class ApiClientError extends Error {
  constructor(message: string, readonly status?: number) {
    super(message);
    this.name = 'ApiClientError';
  }
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), options.timeout ?? 12_000);

  try {
    const response = await fetch(`${API_URL}${path}`, {
      method: options.method ?? 'GET',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
        ...options.headers,
      },
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: controller.signal,
    });
    const payload = await response.json().catch(() => null) as ApiEnvelope<T> | T | { message?: string; detail?: string } | null;

    if (options.responseType === 'raw') {
      if (!response.ok) {
        const problem = payload as { message?: string; detail?: string } | null;
        throw new ApiClientError(problem?.message || problem?.detail || `Máy chủ trả về lỗi ${response.status}.`, response.status);
      }
      return payload as T;
    }

    const envelope = payload as ApiEnvelope<T> | null;
    if (!response.ok || !envelope?.success) {
      throw new ApiClientError(envelope?.message || `Máy chủ trả về lỗi ${response.status}.`, response.status);
    }
    return envelope.data;
  } catch (error) {
    if (error instanceof ApiClientError) throw error;
    if (error instanceof Error && error.name === 'AbortError') {
      throw new ApiClientError('Máy chủ phản hồi quá lâu. Vui lòng thử lại.');
    }
    throw new ApiClientError('Không thể kết nối đến máy chủ. Hãy kiểm tra backend và địa chỉ API.');
  } finally {
    clearTimeout(timeoutId);
  }
}
