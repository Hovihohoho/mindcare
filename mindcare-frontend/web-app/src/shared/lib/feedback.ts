export type ToastKind = "success" | "error" | "info";

export interface ToastDetail {
  message: string;
  kind?: ToastKind;
}

export function notify(message: string, kind: ToastKind = "info") {
  window.dispatchEvent(new CustomEvent<ToastDetail>("mindcare:toast", {
    detail: { message, kind },
  }));
}
