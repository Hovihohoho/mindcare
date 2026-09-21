package com.mindcare.ai_service.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DocumentChunker {
    private final int targetCharacters;
    private final int overlapCharacters;

    public DocumentChunker(@Value("${rag.chunk-size-characters:1800}") int targetCharacters,
                           @Value("${rag.chunk-overlap-characters:250}") int overlapCharacters) {
        if (targetCharacters < 500 || overlapCharacters < 0 || overlapCharacters >= targetCharacters / 2) {
            throw new IllegalArgumentException("Invalid RAG chunk configuration");
        }
        this.targetCharacters = targetCharacters;
        this.overlapCharacters = overlapCharacters;
    }

    public List<Chunk> split(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) return List.of();
        List<Chunk> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int proposedEnd = Math.min(start + targetCharacters, normalized.length());
            int end = boundary(normalized, start, proposedEnd);
            String content = normalized.substring(start, end).trim();
            if (!content.isEmpty()) chunks.add(new Chunk(chunks.size(), content, estimateTokens(content)));
            if (end == normalized.length()) break;
            start = Math.max(start + 1, end - overlapCharacters);
        }
        return List.copyOf(chunks);
    }

    private int boundary(String text, int start, int proposedEnd) {
        if (proposedEnd == text.length()) return proposedEnd;
        int minimum = start + targetCharacters * 2 / 3;
        for (int i = proposedEnd; i >= minimum; i--) {
            char previous = text.charAt(i - 1);
            if (previous == '.' || previous == '!' || previous == '?' || previous == '\n') return i;
        }
        int whitespace = text.lastIndexOf(' ', proposedEnd);
        return whitespace >= minimum ? whitespace : proposedEnd;
    }

    private int estimateTokens(String content) {
        return Math.max(1, (int) Math.ceil(content.length() / 4.0));
    }

    public record Chunk(int index, String content, int tokenEstimate) {}
}
