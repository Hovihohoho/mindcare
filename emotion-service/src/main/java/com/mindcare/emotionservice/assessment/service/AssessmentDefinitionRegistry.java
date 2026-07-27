package com.mindcare.emotionservice.assessment.service;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AssessmentDefinitionRegistry {

    private final Map<AssessmentCode, AssessmentDefinition> definitions;

    public AssessmentDefinitionRegistry() {
        EnumMap<AssessmentCode, AssessmentDefinition> configured = new EnumMap<>(AssessmentCode.class);
        register(configured, definition(AssessmentCode.PHQ_9, 9, phqAndGadScale()));
        register(configured, definition(AssessmentCode.GAD_7, 7, phqAndGadScale()));
        register(configured, definition(AssessmentCode.DASS_21, 21, List.of(
                option("Không đúng với tôi chút nào", 0),
                option("Đúng với tôi ở mức độ nào đó hoặc đôi lúc", 1),
                option("Đúng với tôi ở mức độ đáng kể hoặc phần lớn thời gian", 2),
                option("Hoàn toàn đúng với tôi hoặc hầu hết thời gian", 3)
        )));
        register(configured, definition(AssessmentCode.PSS_10, 10, List.of(
                option("Không bao giờ", 0),
                option("Hầu như không bao giờ", 1),
                option("Thỉnh thoảng", 2),
                option("Khá thường xuyên", 3),
                option("Rất thường xuyên", 4)
        )));
        register(configured, definition(AssessmentCode.WHO_5, 5, List.of(
                option("Không lúc nào", 0),
                option("Một ít thời gian", 1),
                option("Ít hơn một nửa thời gian", 2),
                option("Hơn một nửa thời gian", 3),
                option("Hầu hết thời gian", 4),
                option("Mọi lúc", 5)
        )));
        if (configured.size() != AssessmentCode.values().length) {
            throw new IllegalStateException("Every assessment code must have exactly one definition");
        }
        definitions = Map.copyOf(configured);
    }

    public AssessmentDefinition getRequired(AssessmentCode code) {
        AssessmentDefinition definition = definitions.get(code);
        if (definition == null) {
            throw new InvalidRequestException(
                    "ASSESSMENT_DEFINITION_NOT_CONFIGURED",
                    "No assessment definition is configured for " + code
            );
        }
        return definition;
    }

    private void register(
            Map<AssessmentCode, AssessmentDefinition> configured,
            AssessmentDefinition definition
    ) {
        if (configured.putIfAbsent(definition.code(), definition) != null) {
            throw new IllegalStateException("Duplicate assessment definition for " + definition.code());
        }
    }

    private AssessmentDefinition definition(
            AssessmentCode code,
            int questionCount,
            List<AssessmentDefinition.ResponseScaleOption> scale
    ) {
        return new AssessmentDefinition(code, questionCount, scale);
    }

    private List<AssessmentDefinition.ResponseScaleOption> phqAndGadScale() {
        return List.of(
                option("Không hề", 0),
                option("Vài ngày", 1),
                option("Hơn một nửa số ngày", 2),
                option("Gần như mỗi ngày", 3)
        );
    }

    private AssessmentDefinition.ResponseScaleOption option(String text, int rawValue) {
        return new AssessmentDefinition.ResponseScaleOption(text, rawValue);
    }
}
