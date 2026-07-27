package com.mindcare.bookingservice.integration.auth;

import java.util.List;

public record ExpertDirectoryPage(
        List<ExpertDirectoryItem> items,
        String nextCursor,
        boolean hasMore) {

    public ExpertDirectoryPage {
        items = List.copyOf(items);
    }
}
