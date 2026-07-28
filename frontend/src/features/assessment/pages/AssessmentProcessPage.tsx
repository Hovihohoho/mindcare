import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { Button, Card, Loading } from "@/shared";
import { assessmentApi } from "../api/assessment.api";
import { QuestionPanel } from "../components/QuestionPanel";
import { useAssessment } from "../hooks/useAssessments";

export function AssessmentProcessPage() {
  const { code = "GAD-7" } = useParams();
  const navigate = useNavigate();
  const assessment = useAssessment(code);
  const [index, setIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  if (assessment.isLoading) return <Loading />;
  if (assessment.isError || !assessment.data?.questions?.length) {
    return <Card className="mx-auto max-w-xl p-8 text-center text-rose-700">Không tải được bài đánh giá đã công bố.</Card>;
  }
  const questions = assessment.data.questions;
  const question = questions[index];
  const last = index === questions.length - 1;

  const submit = async () => {
    setSubmitting(true);
    setError("");
    try {
      const payload = questions.map((item) => ({ questionId: item.id, optionId: answers[item.id] }));
      if (payload.some((item) => !item.optionId)) throw new Error("Vui lòng trả lời đầy đủ tất cả câu hỏi.");
      const result = await assessmentApi.submit(code, assessment.data.assessmentVersion, payload);
      navigate(`/assessments/${code}/result`, { state: { result } });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Không thể nộp bài đánh giá.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-[820px]">
      <div className="mb-4 flex items-end justify-between gap-5">
        <h1 className="text-4xl font-bold text-brand-700 md:text-5xl">{assessment.data.title}</h1>
        <p className="shrink-0 text-sm font-semibold text-muted">Câu hỏi {index + 1} / {questions.length}</p>
      </div>
      <div className="mb-10 h-2 overflow-hidden rounded-full bg-slate-200"><div className="h-full rounded-full bg-emerald-700 transition-all" style={{ width: `${((index + 1) / questions.length) * 100}%` }} /></div>
      <Card className="p-7 md:p-10">
        <QuestionPanel question={question} value={answers[question.id]} onChange={(value) => setAnswers((current) => ({ ...current, [question.id]: value }))} />
        {error && <p className="mt-5 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
        <div className="mt-10 flex justify-between pt-2">
          <Button variant="ghost" leftIcon={<ChevronLeft className="size-4" />} disabled={index === 0 || submitting} onClick={() => setIndex((value) => value - 1)}>Quay lại</Button>
          <Button className="min-w-44" leftIcon={<ChevronRight className="size-4" />} loading={submitting} disabled={!answers[question.id]} onClick={() => last ? void submit() : setIndex((value) => value + 1)}>{last ? "Hoàn thành" : "Tiếp theo"}</Button>
        </div>
      </Card>
    </div>
  );
}
