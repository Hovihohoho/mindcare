import type { ReactNode } from "react";
import { Inbox } from "lucide-react";

interface EmptyStateProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="surface grid min-h-64 place-items-center p-8 text-center">
      <div>
        <span className="mx-auto mb-4 grid size-14 place-items-center rounded-2xl bg-slate-100 text-slate-400"><Inbox /></span>
        <h3 className="font-bold text-slate-900">{title}</h3>
        {description && <p className="mx-auto mt-2 max-w-md text-sm text-muted">{description}</p>}
        {action && <div className="mt-5">{action}</div>}
      </div>
    </div>
  );
}
