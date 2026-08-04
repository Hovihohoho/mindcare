package com.mindcare.bookingservice.chat.mapper;

import com.mindcare.bookingservice.chat.dto.MessageResponse;
import com.mindcare.bookingservice.chat.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ChatMapper {

    MessageResponse toResponse(Message message);
}
