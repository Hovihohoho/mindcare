package com.mindcare.ai_service.service;

import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TrustedSourcePolicy {
    private static final List<String> TIER_A = List.of(
            "who.int", "nih.gov", "cdc.gov", "moh.gov.vn", "nhs.uk", "developer.android.com");
    private static final List<String> TIER_B = List.of(
            "doi.org", "pubmed.ncbi.nlm.nih.gov", "ncbi.nlm.nih.gov", "jamanetwork.com");

    public SourceTrust verify(String sourceUrl) {
        try {
            URI uri = URI.create(sourceUrl);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null) throw unsupported();
            host = host.toLowerCase();
            if (matches(host, TIER_A)) return new SourceTrust("A", publisher(host));
            if (matches(host, TIER_B)) return new SourceTrust("B", publisher(host));
            throw unsupported();
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Nguồn chưa")) throw exception;
            throw new IllegalArgumentException("Đường dẫn nguồn không hợp lệ", exception);
        }
    }

    private boolean matches(String host, List<String> domains) {
        return domains.stream().anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
    }

    private String publisher(String host) {
        if (host.endsWith("who.int")) return "Tổ chức Y tế Thế giới (WHO)";
        if (host.endsWith("nih.gov")) return "Viện Y tế Quốc gia Hoa Kỳ (NIH)";
        if (host.endsWith("cdc.gov")) return "Trung tâm Kiểm soát và Phòng ngừa Dịch bệnh Hoa Kỳ (CDC)";
        if (host.endsWith("moh.gov.vn")) return "Bộ Y tế Việt Nam";
        if (host.endsWith("nhs.uk")) return "Dịch vụ Y tế Quốc gia Anh (NHS)";
        if (host.equals("doi.org") || host.endsWith("jamanetwork.com")) return "Công trình khoa học có định danh DOI";
        if (host.endsWith("ncbi.nlm.nih.gov")) return "Thư viện Y khoa Quốc gia Hoa Kỳ";
        if (host.equals("developer.android.com")) return "Android Developers";
        return host;
    }

    private IllegalArgumentException unsupported() {
        return new IllegalArgumentException("Nguồn chưa thuộc danh sách uy tín được MindCare hỗ trợ");
    }

    public record SourceTrust(String tier, String publisher) {}
}
