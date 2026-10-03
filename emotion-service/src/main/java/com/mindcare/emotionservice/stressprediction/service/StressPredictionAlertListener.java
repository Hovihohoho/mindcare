package com.mindcare.emotionservice.stressprediction.service;

import com.mindcare.emotionservice.risk.client.NotificationIntentClient;
import com.mindcare.emotionservice.stressprediction.event.StressPredictionAlertEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StressPredictionAlertListener {
    private static final Logger log = LoggerFactory.getLogger(StressPredictionAlertListener.class);

    private final NotificationIntentClient notificationIntentClient;
    private final StressPredictionRecordService recordService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterPredictionCommitted(StressPredictionAlertEvent event) {
        try {
            notificationIntentClient.requestStressPrediction(
                    event.userId(), event.predictionId(), event.stressScore(), event.alertLevel());
            recordService.markNotified(event.predictionId(), event.stressScore());
        } catch (RuntimeException exception) {
            log.warn("PMData stress notification failed for predictionId={} userId={}",
                    event.predictionId(), event.userId(), exception);
        }
    }
}
