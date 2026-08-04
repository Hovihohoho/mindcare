package com.mindcare.bookingservice.booking.service;

public interface BookingMaintenanceService {

    int expirePaymentHolds();

    int expireCancellationRequests();

    int createConsultationReminders();
}
