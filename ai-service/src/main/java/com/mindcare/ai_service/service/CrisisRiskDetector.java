package com.mindcare.ai_service.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CrisisRiskDetector {
    private static final Pattern EXPLICIT = Pattern.compile(
            "\\b(tu sat|tu hai|hai ban than|muon chet|khong muon song|khong muon tiep tuc song|"
            + "ket thuc cuoc doi|tu tu|gieo minh|cat tay|overdose|suicide|kill myself|hurt myself)\\b");
    private static final Pattern AMBIGUOUS = Pattern.compile(
            "\\b(tuyet vong|khong con ly do de song|song khong co y nghia|khong chiu noi nua|"
            + "muon bien mat|be tac hoan toan|vo dung|ganh nang cho moi nguoi)\\b");
    private static final Pattern IMMINENT = Pattern.compile(
            "\\b(ngay bay gio|toi nay|hom nay|da co ke hoach|da chuan bi|sap lam|"
            + "khong the giu an toan|co dao|co sung|co thuoc|vua tu hai)\\b");

    public RiskLevel detect(String message) {
        String normalized = normalize(message);
        if (EXPLICIT.matcher(normalized).find()) {
            return IMMINENT.matcher(normalized).find() ? RiskLevel.IMMINENT : RiskLevel.EXPLICIT;
        }
        return AMBIGUOUS.matcher(normalized).find() ? RiskLevel.CHECK_IN : RiskLevel.NONE;
    }

    private String normalize(String input) {
        String value = input == null ? "" : input.toLowerCase(Locale.ROOT);
        value = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return value.replace('đ', 'd').replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    public enum RiskLevel {
        NONE, CHECK_IN, EXPLICIT, IMMINENT;

        public boolean requiresSafetyContext() { return this != NONE; }
    }
}
