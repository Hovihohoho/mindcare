import type { ReactNode } from "react";

interface AuthFormShellProps {
  title: string;
  description: string;
  children: ReactNode;
  footer?: ReactNode;
}

export function AuthFormShell({ title, description, children, footer }: AuthFormShellProps) {
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900 md:text-4xl">{title}</h1>
        <p className="mt-3 leading-7 text-slate-500">{description}</p>
      </div>
      {children}
      {footer && <div className="mt-7 text-center text-sm text-muted">{footer}</div>}
    </div>
  );
}
