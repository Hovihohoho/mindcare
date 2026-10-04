import { ApiClientError } from './api.client';

type CursorPage<T> = { items: T[]; nextCursor: string | null; hasMore: boolean };

export async function collectCursorPages<T>(load: (cursor?: string) => Promise<CursorPage<T>>): Promise<CursorPage<T>> {
  const items: T[] = [];
  const seen = new Set<string>();
  let cursor: string | undefined;
  while (true) {
    const page = await load(cursor);
    items.push(...page.items);
    if (!page.hasMore) return { items, nextCursor: null, hasMore: false };
    if (!page.nextCursor || seen.has(page.nextCursor)) {
      throw new ApiClientError('Không thể tải đầy đủ lịch sử. Vui lòng thử lại.', 502);
    }
    seen.add(page.nextCursor);
    cursor = page.nextCursor;
  }
}
