package com.mindcare.emotionservice.risk.service;

import com.mindcare.emotionservice.assessment.service.AssessmentSubmitted;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AssessmentRiskListener {
    private final RiskService risk;

    public AssessmentRiskListener(RiskService risk) { this.risk = risk; }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onSubmitted(AssessmentSubmitted event) {
        risk.analyzeAssessmentResult(event.userId(), event.result());
    }
}
