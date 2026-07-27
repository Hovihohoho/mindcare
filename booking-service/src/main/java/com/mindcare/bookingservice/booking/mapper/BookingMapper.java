package com.mindcare.bookingservice.booking.mapper;

import com.mindcare.bookingservice.booking.dto.BookingResponse;
import com.mindcare.bookingservice.booking.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BookingMapper {

    BookingResponse toResponse(Booking booking);
}
