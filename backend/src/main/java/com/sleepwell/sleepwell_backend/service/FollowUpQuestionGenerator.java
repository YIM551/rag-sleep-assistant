package com.sleepwell.sleepwell_backend.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class FollowUpQuestionGenerator {

    public List<String> generate(String message) {
        List<String> questions = new ArrayList<>();

        String normalized = message == null ? "" : message.toLowerCase(Locale.KOREAN);

        if (normalized.contains("커피") || normalized.contains("카페인")) {
            questions.add("카페인을 주로 어느 시간대에 섭취하시나요?");
            questions.add("카페인을 끊었을 때 수면이 좋아진 경험이 있었나요?");
        }

        if (normalized.contains("낮잠")) {
            questions.add("낮잠을 보통 몇 시쯤, 얼마나 주무시나요?");
            questions.add("낮잠을 잔 날 밤수면이 더 어려웠던가요?");
        }

        if (normalized.contains("졸리") || normalized.contains("피곤")) {
            questions.add("최근 며칠간 평균 수면 시간이 얼마나 되었나요?");
            questions.add("오후나 저녁에 졸림이 특히 심해지는 시간이 있나요?");
        }

        if (normalized.contains("잠이 안") || normalized.contains("잠이 잘") || normalized.contains("불면")) {
            questions.add("잠들기 전 루틴이나 습관이 어떤 편인가요?");
            questions.add("잠들기까지 보통 몇 분 정도 걸리나요?");
        }

        if (questions.isEmpty()) {
            questions.add("최근 수면 패턴에서 가장 불편한 점은 무엇인가요?");
            questions.add("생활 패턴(취침/기상 시간)에 변화가 있었나요?");
        }

        return questions.stream().distinct().limit(3).toList();
    }
}
