import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { Button, Card, Loading } from "@/shared";
import { assessmentApi } from "../api/assessment.api";
import { QuestionPanel } from "../components/QuestionPanel";
import { useAssessment } from "../hooks/useAssessments";

export function AssessmentProcessPage() {
  const { code = "PHQ-9" } = useParams();
  const navigate = useNavigate();
  const assessment = useAssessment(code);
  const [index, setIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const submit = useMutation({
    mutationFn: () => assessmentApi.submit(
      code,
      assessment.data!.assessmentVersion,
      assessment.data!.questions!.map((item) => ({
        questionId: item.id,
        optionId: answers[item.id],
      })),
    ),
    onSuccess: (result) => navigate(`/assessments/${code}/result?resultId=${result.resultId}`),
  });
  if (assessment.isLoading || !assessment.data?.questions) return <Loading />;
  const questions = assessment.data.questions;
  const question = questions[index];
  const last = index === questions.length - 1;

  return (
    <div className="mx-auto max-w-[820px]">
      <div className="mb-4 flex items-end justify-between gap-5">
        <h1 className="text-4xl font-bold text-brand-700 md:text-5xl">{assessment.data.title}</h1>
        <p className="shrink-0 text-sm font-semibold text-muted">Câu hỏi {index + 1} / {questions.length}</p>
      </div>
      <div className="mb-10 h-2 overflow-hidden rounded-full bg-slate-200"><div className="h-full rounded-full bg-emerald-700 transition-all" style={{ width: `${((index + 1) / questions.length) * 100}%` }} /></div>
      <Card className="p-7 md:p-10">
        <QuestionPanel question={question} value={answers[question.id]} onChange={(value) => setAnswers((current) => ({ ...current, [question.id]: value }))} />
        <div className="mt-10 flex justify-between pt-2">
          <Button variant="ghost" leftIcon={<ChevronLeft className="size-4" />} disabled={index === 0} onClick={() => setIndex((value) => value - 1)}>Quay lại</Button>
          <Button
            className="min-w-44"
            leftIcon={<ChevronRight className="size-4" />}
            disabled={!answers[question.id] || submit.isPending}
            loading={submit.isPending}
            onClick={() => last ? submit.mutate() : setIndex((value) => value + 1)}
          >
            {last ? "Hoàn thành" : "Tiếp theo"}
          </Button>
        </div>
        {submit.isError && <p className="mt-4 text-sm text-rose-700">Không thể gửi bài đánh giá. Vui lòng thử lại.</p>}
      </Card>
    </div>
  );
}
