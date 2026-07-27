import { UploadCloud } from "lucide-react";

export function UploadField({ label, multiple }: { label: string; multiple?: boolean }) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-bold text-slate-700">{label}</span>
      <span className="flex min-h-32 cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-line bg-slate-50 p-5 text-center transition hover:border-brand-500">
        <UploadCloud className="size-7 text-brand-600" /><b className="mt-2 text-sm">Kéo thả file hoặc Click để tải lên</b><small className="mt-1 text-muted">{multiple ? "Hỗ trợ nhiều file PDF, JPG, PNG" : "PDF, JPG hoặc PNG · Tối đa 10MB"}</small>
        <input className="sr-only" type="file" multiple={multiple} />
      </span>
    </label>
  );
}
