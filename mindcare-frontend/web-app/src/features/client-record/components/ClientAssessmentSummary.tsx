import { Badge, Card } from "@/shared";

const results = [
  { title: "PHQ-9 (Trầm cảm)", score: "12 / 27", level: "Trung bình", tone: "warning" as const, date: "15/09/2024" },
  { title: "GAD-7 (Lo âu)", score: "14 / 21", level: "Trung bình", tone: "warning" as const, date: "15/09/2024" },
  { title: "DASS-21 (Căng thẳng)", score: "08 / 42", level: "Thấp", tone: "success" as const, date: "10/08/2024" },
];

export function ClientAssessmentSummary() {
  return <section><h2 className="border-b border-line px-6 py-4 text-lg font-medium text-blue-600">Kết quả khảo sát</h2><div className="mx-auto mt-6 grid max-w-[960px] gap-6 md:grid-cols-3">{results.map((result) => <Card className="p-6" key={result.title}><div className="flex items-start justify-between gap-2"><h3 className="font-bold">{result.title}</h3><Badge tone={result.tone}>{result.level}</Badge></div><b className="mt-5 block text-2xl text-blue-700">{result.score}</b><p className="mt-2 text-xs text-muted">Lần cuối: {result.date}</p></Card>)}</div></section>;
}
