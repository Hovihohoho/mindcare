package com.mindcare.ai_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static com.mindcare.ai_service.service.CrisisRiskDetector.RiskLevel.*;
import org.junit.jupiter.api.Test;

class CrisisRiskDetectorTest {
    private final CrisisRiskDetector detector = new CrisisRiskDetector();

    @Test
    void insomniaAloneDoesNotTriggerCrisisRouting() {
        assertThat(detector.detect("Khó ngủ, trằn trọc đến 3 giờ sáng mới ngủ được")).isEqualTo(NONE);
        assertThat(detector.detect("Dạo này tôi mệt vì ngủ không ngon")).isEqualTo(NONE);
    }

    @Test
    void distinguishesAmbiguousExplicitAndImminentSignals() {
        assertThat(detector.detect("Tôi thấy tuyệt vọng và bế tắc hoàn toàn")).isEqualTo(CHECK_IN);
        assertThat(detector.detect("Tôi đang nghĩ đến việc tự sát")).isEqualTo(EXPLICIT);
        assertThat(detector.detect("Tôi muốn chết và đã có kế hoạch tối nay")).isEqualTo(IMMINENT);
    }
}
