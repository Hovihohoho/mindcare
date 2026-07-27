import type { ReactNode } from "react";

interface SettingRowProps {
  title: string;
  description: string;
  control: ReactNode;
}

export function SettingRow({ title, description, control }: SettingRowProps) {
  return <div className="flex items-center justify-between gap-5 border-b border-line py-5 last:border-0"><div><h3 className="font-bold">{title}</h3><p className="mt-1 text-sm text-muted">{description}</p></div>{control}</div>;
}
