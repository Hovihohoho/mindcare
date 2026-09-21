package com.mindcare.ai_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentChunkerTest {
    private final DocumentChunker chunker = new DocumentChunker(500, 80);

    @Test
    void splitsLongTextAtSentenceBoundariesWithStableIndexes() {
        String paragraph = "Căng thẳng kéo dài có thể ảnh hưởng đến giấc ngủ và khả năng tập trung. ";
        var chunks = chunker.split(paragraph.repeat(30));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).extracting(DocumentChunker.Chunk::index)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, chunks.size()).boxed().toList());
        assertThat(chunks).allMatch(chunk -> chunk.content().length() <= 500);
        assertThat(chunks).allMatch(chunk -> chunk.tokenEstimate() > 0);
    }

    @Test
    void normalizesWhitespaceAndHandlesEmptyInput() {
        assertThat(chunker.split("  Nội dung   hợp lệ.  ")).singleElement()
                .extracting(DocumentChunker.Chunk::content).isEqualTo("Nội dung hợp lệ.");
        assertThat(chunker.split("  ")).isEmpty();
    }
}
