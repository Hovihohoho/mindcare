package com.mindcare.emotionservice.assessment.config;

import com.mindcare.emotionservice.assessment.dto.AdminAssessmentResponse;
import com.mindcare.emotionservice.assessment.dto.UpsertAssessmentRequest;
import com.mindcare.emotionservice.assessment.dto.UpsertQuestionRequest;
import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.assessment.repository.AssessmentRepository;
import com.mindcare.emotionservice.assessment.service.AssessmentService;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@ConditionalOnProperty(name = "mindcare.seed-assessments", havingValue = "true")
public class AssessmentCatalogSeeder implements ApplicationRunner {
    private final AssessmentRepository assessmentRepository;
    private final AssessmentService assessmentService;

    public AssessmentCatalogSeeder(
            AssessmentRepository assessmentRepository,
            AssessmentService assessmentService) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentService = assessmentService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedIfMissing(
                AssessmentCode.PHQ_9,
                "PHQ-9 - Sàng lọc triệu chứng trầm cảm",
                "Trong hai tuần vừa qua, hãy chọn mức độ thường xuyên bạn gặp từng vấn đề. "
                        + "Kết quả chỉ mang tính sàng lọc, không thay thế chẩn đoán y khoa.",
                List.of(
                        "Ít hứng thú hoặc không còn thấy vui khi làm mọi việc.",
                        "Cảm thấy buồn bã, chán nản hoặc tuyệt vọng.",
                        "Khó ngủ, ngủ không sâu hoặc ngủ quá nhiều.",
                        "Cảm thấy mệt mỏi hoặc thiếu năng lượng.",
                        "Ăn kém ngon hoặc ăn quá nhiều.",
                        "Cảm thấy bản thân tồi tệ, thất bại hoặc làm gia đình thất vọng.",
                        "Khó tập trung vào công việc, đọc sách hoặc xem truyền hình.",
                        "Di chuyển hoặc nói chậm, hoặc bồn chồn nhiều hơn bình thường.",
                        "Có ý nghĩ rằng thà mình không còn sống hoặc muốn làm tổn thương bản thân."));
        seedIfMissing(
                AssessmentCode.GAD_7,
                "GAD-7 - Sàng lọc triệu chứng lo âu",
                "Trong hai tuần vừa qua, hãy chọn mức độ thường xuyên bạn bị làm phiền bởi từng vấn đề. "
                        + "Kết quả chỉ mang tính sàng lọc, không thay thế chẩn đoán y khoa.",
                List.of(
                        "Cảm thấy lo lắng, bồn chồn hoặc căng thẳng",
                        "Không thể dừng hoặc kiểm soát sự lo lắng",
                        "Lo lắng quá nhiều về nhiều việc khác nhau",
                        "Khó thư giãn",
                        "Bồn chồn đến mức khó ngồi yên",
                        "Dễ trở nên khó chịu hoặc cáu kỉnh",
                        "Cảm thấy sợ hãi như thể điều khủng khiếp có thể xảy ra"));
        seedIfMissing(
                AssessmentCode.WHO_5,
                "WHO-5 - Chỉ số sức khỏe tinh thần",
                "Trong hai tuần vừa qua, hãy chọn mức độ phù hợp với trạng thái tích cực của bạn. "
                        + "Điểm thấp cho thấy bạn nên trao đổi thêm với chuyên gia.",
                List.of(
                        "Tôi cảm thấy vui vẻ và có tinh thần tốt",
                        "Tôi cảm thấy bình tĩnh và thư thái",
                        "Tôi cảm thấy năng động và tràn đầy sức sống",
                        "Tôi thức dậy với cảm giác khỏe khoắn và được nghỉ ngơi",
                        "Cuộc sống hằng ngày của tôi có nhiều điều khiến tôi quan tâm"));
    }

    private void seedIfMissing(
            AssessmentCode code,
            String title,
            String description,
            List<String> questionTexts) {
        if (assessmentRepository.existsByCodeAndDeletedAtIsNull(code)) {
            return;
        }
        List<UpsertQuestionRequest> questions = java.util.stream.IntStream
                .range(0, questionTexts.size())
                .mapToObj(index -> new UpsertQuestionRequest(questionTexts.get(index), index))
                .toList();
        AdminAssessmentResponse draft = assessmentService.createAssessment(
                new UpsertAssessmentRequest(code, title, description, questions));
        assessmentService.publishAssessment(draft.id());
    }
}
