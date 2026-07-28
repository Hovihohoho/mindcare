import { Button } from "../Button/Button";

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description: string;
  onCancel: () => void;
  onConfirm: () => void;
}

export function ConfirmDialog({ open, title, description, onCancel, onConfirm }: ConfirmDialogProps) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-[100] grid place-items-center bg-slate-950/40 p-4" role="dialog" aria-modal="true">
      <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
        <h2 className="text-xl font-bold">{title}</h2>
        <p className="mt-2 text-sm leading-6 text-muted">{description}</p>
        <div className="mt-6 flex justify-end gap-3">
          <Button onClick={onCancel} variant="outline">Hủy</Button>
          <Button onClick={onConfirm} variant="danger">Đăng xuất</Button>
        </div>
      </div>
    </div>
  );
}
