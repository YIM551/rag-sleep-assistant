package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.enums.RecommendedFeature;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class RecommendedFeatureDetector {

    private static final String[] SLEEPY_KEYWORDS = {"졸리", "졸림", "피곤"};
    private static final String[] NEGATION_KEYWORDS = {"안", "않", "별로", "전혀", "못"};
    private static final int NEGATION_WINDOW = 5;
    private static final Pattern NEGATED_SLEEPY_PATTERN = Pattern.compile(
            "(안\\s*졸리|전혀\\s*졸리|별로\\s*졸리|졸리(지|진)?\\s*않|졸리(지|진)?\\s*못|졸린\\s*게\\s*아니|졸린\\s*건\\s*아니|졸린\\s*것\\s*아니)");

    public RecommendedFeature detect(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        String normalized = message.toLowerCase(Locale.KOREAN);

        if (normalized.contains("잠이 안") || normalized.contains("잠이 잘 안") || normalized.contains("입면")
                || normalized.contains("불면") || normalized.contains("잠들기 어렵")) {
            return RecommendedFeature.ASMR;
        }

        if (containsAny(normalized, SLEEPY_KEYWORDS) && !hasNegationNear(normalized, SLEEPY_KEYWORDS)
                && !hasNegatedSleepiness(normalized)) {
            return RecommendedFeature.POWER_NAP_15M;
        }

        return null;
    }

    private boolean containsAny(String message, String[] keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword.toLowerCase(Locale.KOREAN))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNegationNear(String message, String[] keywords) {
        for (String keyword : keywords) {
            int index = message.indexOf(keyword);
            while (index >= 0) {
                int start = Math.max(0, index - NEGATION_WINDOW);
                int end = Math.min(message.length(), index + keyword.length() + NEGATION_WINDOW);
                String window = message.substring(start, end);
                if (containsAny(window, NEGATION_KEYWORDS)) {
                    return true;
                }
                index = message.indexOf(keyword, index + 1);
            }
        }
        return false;
    }

    private boolean hasNegatedSleepiness(String message) {
        return NEGATED_SLEEPY_PATTERN.matcher(message).find();
    }
}
