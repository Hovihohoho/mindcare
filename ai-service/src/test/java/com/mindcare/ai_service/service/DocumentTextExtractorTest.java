package com.mindcare.ai_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTextExtractorTest {
    private final DocumentTextExtractor extractor = new DocumentTextExtractor();

    @Test
    void extractsUtf8TextFile() throws Exception {
        var file = new MockMultipartFile("file", "guide.txt", "text/plain",
                "Hướng dẫn chăm sóc sức khỏe tinh thần mỗi ngày.".getBytes(StandardCharsets.UTF_8));
        assertThat(extractor.extract(file)).contains("sức khỏe tinh thần");
    }

    @Test
    void extractsDocxDocumentXml() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                      <w:body><w:p><w:r><w:t>Nội dung tư vấn tâm lý an toàn</w:t></w:r></w:p></w:body>
                    </w:document>
                    """.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        var file = new MockMultipartFile("file", "guide.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                bytes.toByteArray());
        assertThat(extractor.extract(file)).contains("tư vấn tâm lý");
    }

    @Test
    void rejectsUnsupportedFileType() {
        var file = new MockMultipartFile("file", "image.png", "image/png", new byte[100]);
        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("TXT, PDF hoặc DOCX");
    }
}
