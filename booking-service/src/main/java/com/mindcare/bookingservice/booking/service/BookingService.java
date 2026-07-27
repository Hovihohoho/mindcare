package com.mindcare.bookingservice.booking.service;

import com.mindcare.bookingservice.booking.dto.BookingCheckoutResponse;
import com.mindcare.bookingservice.booking.dto.BookingResponse;
import com.mindcare.bookingservice.booking.dto.CancellationDecisionRequest;
import com.mindcare.bookingservice.booking.dto.CancellationRequest;
import com.mindcare.bookingservice.booking.dto.CreateBookingRequest;
import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import java.util.UUID;

public interface BookingService {

    BookingCheckoutResponse createCheckout(
            UUID userId,
            String idempotencyKey,
            CreateBookingRequest request);

    BookingResponse getForUser(UUID userId, UUID bookingId);

    BookingResponse getForExpert(UUID expertUserId, UUID bookingId);

    CursorPageResponse<BookingResponse> getUserHistory(
            UUID userId,
            BookingStatus status,
            String cursor,
            int limit);

    CursorPageResponse<BookingResponse> getExpertHistory(
            UUID expertUserId,
            BookingStatus status,
            String cursor,
            int limit);

    BookingResponse requestUserCancellation(
            UUID userId,
            UUID bookingId,
            CancellationRequest request);

    BookingResponse decideCancellation(
            UUID expertUserId,
            UUID bookingId,
            CancellationDecisionRequest request);

    BookingResponse cancelByExpert(
            UUID expertUserId,
            UUID bookingId,
            CancellationRequest request);

    BookingResponse complete(UUID expertUserId, UUID bookingId);

    BookingResponse markUserNoShow(UUID expertUserId, UUID bookingId);
}
