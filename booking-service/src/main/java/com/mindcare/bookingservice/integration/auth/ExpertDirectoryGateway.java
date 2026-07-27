package com.mindcare.bookingservice.integration.auth;

public interface ExpertDirectoryGateway {

    ExpertDirectoryPage listBookable(ExpertDirectoryQuery query);
}
