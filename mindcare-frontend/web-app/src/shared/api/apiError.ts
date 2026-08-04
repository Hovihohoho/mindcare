export function getApiErrorMessage(error: unknown, fallback: string) {
  const responseMessage = (error as { response?: { data?: { message?: unknown } } })
    ?.response?.data?.message;
  return typeof responseMessage === "string" && responseMessage.trim()
    ? responseMessage
    : fallback;
}
