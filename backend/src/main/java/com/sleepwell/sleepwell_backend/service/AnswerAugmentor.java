package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.enums.RecommendedFeature;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnswerAugmentor {

    public String appendFeatureRecommendation(String answer, RecommendedFeature feature) {
        if (feature == null || answer == null) {
            return answer;
        }

        if (answer.contains("앱 기능 추천")) {
            return answer;
        }

        StringBuilder builder = new StringBuilder(answer);
        builder.append("\n\n### 앱 기능 추천\n");
        if (feature == RecommendedFeature.ASMR) {
            builder.append("- 지금은 입면을 돕기 위해 **ASMR 사운드**를 바로 켜보는 것을 추천해요. ");
            builder.append("앱의 ASMR 메뉴에서 편안한 트랙을 선택해보세요.\n");
        } else if (feature == RecommendedFeature.POWER_NAP_15M) {
            builder.append("- 졸림이 심할 때는 **15분 쪽잠 알람**을 맞추고 짧게 쉬는 게 좋아요. ");
            builder.append("앱에서 15분 파워냅 타이머를 바로 실행해보세요.\n");
        }
        return builder.toString();
    }

    public String appendFollowUpQuestions(String answer, List<String> followUpQuestions) {
        if (answer == null || followUpQuestions == null || followUpQuestions.isEmpty()) {
            return answer;
        }

        if (answer.contains("꼬리질문")) {
            return answer;
        }

        StringBuilder builder = new StringBuilder(answer);
        builder.append("\n\n### 꼬리질문\n");
        for (int i = 0; i < followUpQuestions.size(); i++) {
            builder.append(i + 1).append(") ").append(followUpQuestions.get(i)).append("\n");
        }
        return builder.toString().trim();
    }
}
