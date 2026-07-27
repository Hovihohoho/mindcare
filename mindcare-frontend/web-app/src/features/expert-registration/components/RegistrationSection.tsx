import type { ReactNode } from "react";
import { Card } from "@/shared";

interface RegistrationSectionProps {
  step: string;
  title: string;
  description: string;
  children: ReactNode;
}

export function RegistrationSection({ step, title, description, children }: RegistrationSectionProps) {
  return (
    <Card className="overflow-hidden">
      <header className="flex gap-4 border-b border-line p-6">
        <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-600 font-black text-white">{step}</span>
        <div><h2 className="text-lg font-extrabold">{title}</h2><p className="mt-1 text-sm text-muted">{description}</p></div>
      </header>
      <div className="p-6">{children}</div>
    </Card>
  );
}
