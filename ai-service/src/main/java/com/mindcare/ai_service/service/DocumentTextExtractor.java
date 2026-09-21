package com.mindcare.ai_service.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class DocumentTextExtractor {
    private static final Set<String> TYPES = Set.of(
            "text/plain",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public String extract(MultipartFile file) throws Exception {
        if (file.isEmpty() || file.getSize() > 15 * 1024 * 1024) {
            throw new RuntimeException("Tệp không được để trống và không được vượt quá 15MB");
        }
        if (!TYPES.contains(file.getContentType())) {
            throw new RuntimeException("Chỉ hỗ trợ tệp TXT, PDF hoặc DOCX");
        }
        String text = switch (file.getContentType()) {
            case "application/pdf" -> pdf(file);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> docx(file);
            default -> new String(file.getBytes(), StandardCharsets.UTF_8);
        };
        text = text.replace('\u0000', ' ').replaceAll("[\\t ]+", " ")
                .replaceAll("\\R{3,}", "\n\n").trim();
        if (text.length() < 20) {
            throw new RuntimeException("Không đọc được đủ nội dung chữ từ tài liệu");
        }
        if (text.length() > 1_000_000) {
            throw new RuntimeException("Nội dung tài liệu vượt quá giới hạn 1.000.000 ký tự");
        }
        return text;
    }

    private String pdf(MultipartFile file) throws Exception {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            if (document.isEncrypted()) throw new RuntimeException("Không hỗ trợ PDF có mật khẩu");
            return new PDFTextStripper().getText(document);
        }
    }

    private String docx(MultipartFile file) throws Exception {
        byte[] xml = null;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    xml = zip.readAllBytes();
                    break;
                }
            }
        }
        if (xml == null) throw new RuntimeException("Tệp DOCX không hợp lệ");
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        return document.getDocumentElement().getTextContent();
    }
}
