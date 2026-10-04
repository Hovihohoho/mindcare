export function createRequestId(): string {
  return globalThis.crypto?.randomUUID?.() ?? 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (character) => {
    const value = Math.floor(Math.random() * 16);
    return (character === 'x' ? value : (value & 3) | 8).toString(16);
  });
}

export function createRetrySubmission<T>() {
  let current: { requestId: string; payload: T; fingerprint: string } | null = null;
  return {
    begin(payload: T) {
      const fingerprint = JSON.stringify(payload);
      if (!current || current.fingerprint !== fingerprint) {
        current = { requestId: createRequestId(), payload: JSON.parse(fingerprint) as T, fingerprint };
      }
      return current;
    },
    retry: () => current,
    clear() { current = null; },
  };
}
