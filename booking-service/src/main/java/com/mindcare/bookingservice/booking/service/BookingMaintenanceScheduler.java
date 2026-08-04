package com.mindcare.bookingservice.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingMaintenanceScheduler {

    private final BookingMaintenanceService bookingMaintenanceService;

    @Scheduled(fixedDelayString = "${booking.maintenance.fixed-delay:30000}")
    public void releaseExpiredState() {
        bookingMaintenanceService.expirePaymentHolds();
        bookingMaintenanceService.expireCancellationRequests();
        bookingMaintenanceService.createConsultationReminders();
    }
}
