export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

export interface CursorPage<T> {
  items: T[];
  nextCursor?: string;
  hasMore: boolean;
}
