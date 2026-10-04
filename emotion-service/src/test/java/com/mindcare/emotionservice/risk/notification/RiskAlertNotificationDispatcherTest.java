package com.mindcare.emotionservice.risk.notification;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindcare.emotionservice.risk.entity.PsychologicalAlertLogEntity;
import com.mindcare.emotionservice.risk.repository.PsychologicalAlertLogRepository;
import com.mindcare.emotionservice.risk.service.RiskService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiskAlertNotificationDispatcherTest {

    @Mock private PsychologicalAlertLogRepository repository;
    @Mock private RiskAlertNotificationClient client;
    @Mock private RiskService riskService;
    @Mock private PsychologicalAlertLogEntity alert;

    private RiskAlertNotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new RiskAlertNotificationDispatcher(repository, client, riskService, true);
    }

    @Test
    void marksAlertNotifiedOnlyAfterAuthServiceAcceptsIt() {
        UUID alertId = UUID.randomUUID();
        when(repository.findByIdAndDeletedAtIsNull(alertId)).thenReturn(Optional.of(alert));
        when(alert.getId()).thenReturn(alertId);
        when(alert.getNotified()).thenReturn(false);

        dispatcher.deliver(alertId);

        verify(client).send(alert);
        verify(riskService).markAlertNotified(alertId);
    }

    @Test
    void leavesAlertPendingWhenDeliveryFails() {
        UUID alertId = UUID.randomUUID();
        when(repository.findByIdAndDeletedAtIsNull(alertId)).thenReturn(Optional.of(alert));
        when(alert.getId()).thenReturn(alertId);
        when(alert.getNotified()).thenReturn(false);
        org.mockito.Mockito.doThrow(new IllegalStateException("auth unavailable"))
                .when(client).send(alert);

        dispatcher.deliver(alertId);

        verify(riskService, never()).markAlertNotified(alertId);
    }
}
