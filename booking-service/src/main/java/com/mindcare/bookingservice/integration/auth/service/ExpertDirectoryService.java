package com.mindcare.bookingservice.integration.auth.service;

import com.mindcare.bookingservice.integration.auth.dto.ExpertSummaryResponse;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;

public interface ExpertDirectoryService {

    CursorPageResponse<ExpertSummaryResponse> listBookable(
            String keyword,
            String specialty,
            String cursor,
            int limit);
}
