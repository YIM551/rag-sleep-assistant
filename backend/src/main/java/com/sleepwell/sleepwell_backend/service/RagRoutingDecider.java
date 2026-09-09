package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.enums.RagRouteType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
public class RagRoutingDecider {

    private static final String[] RAG_KEYWORDS = {
            "논문", "근거", "가이드라인", "수치", "퍼센트", "정확", "통계", "연구", "의학", "메타분석"
    };

    private static final String[] NON_RAG_KEYWORDS = {
            "잠이 안 와", "잠이 잘 오지", "피곤", "졸리", "낮잠", "루틴", "습관", "스트레스", "일상"
    };

    public RagRouteType decide(String message) {
        if (message == null || message.isBlank()) {
            return RagRouteType.NON_RAG;
        }

        String normalized = message.toLowerCase(Locale.KOREAN);

        if (containsAny(normalized, RAG_KEYWORDS)) {
            return RagRouteType.RAG;
        }

        if (containsAny(normalized, NON_RAG_KEYWORDS)) {
            return RagRouteType.NON_RAG;
        }

        return RagRouteType.HYBRID;
    }

    private boolean containsAny(String message, String[] keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword.toLowerCase(Locale.KOREAN))) {
                return true;
            }
        }
        return false;
    }
}
