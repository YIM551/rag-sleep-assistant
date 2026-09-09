package com.sleepwell.sleepwell_backend.service.rule;

import com.sleepwell.sleepwell_backend.dto.InsightDto;
import com.sleepwell.sleepwell_backend.dto.SleepAudioTrendAnalysisDto;
import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
public class SnoringTrendRule implements InsightRule {

    private static final double SIGNIFICANT_INCREASE_THRESHOLD = 20.0; // 20% 이상 증가 시

    @Override
    public boolean supports(SleepAudioTrendAnalysisDto analysisData) {
        if (analysisData == null || analysisData.getTrendIndicators() == null) {
            return false;
        }

        // 코골이 이벤트 타입에 대한 트렌드 데이터가 있는지 확인
        return analysisData.getEventTypeAggregations().stream()
                .anyMatch(agg -> agg.getEventType() == AudioEventType.SNORING);
    }

    @Override
    public Optional<InsightDto> generate(SleepAudioTrendAnalysisDto analysisData) {
        SleepAudioTrendAnalysisDto.TrendIndicators trend = analysisData.getTrendIndicators();

        if ("INCREASING".equals(trend.getOverallTrendDirection()) && trend.getChangePercentage() >= SIGNIFICANT_INCREASE_THRESHOLD) {
            
            String message = String.format(
                "최근 수면 데이터 분석 결과, 코골이 발생 빈도가 이전 기간에 비해 약 %.0f%% 증가했습니다. 이는 수면의 질에 영향을 줄 수 있습니다. 수면 자세를 바꾸거나 베개를 조절해보는 것을 권장합니다.",
                trend.getChangePercentage()
            );

            InsightDto insight = InsightDto.builder()
                .code("SNORING_INCREASED_SIGNIFICANTLY")
                .severity(InsightDto.Severity.WARNING)
                .message(message)
                .relatedEventTypes(Collections.singletonList(AudioEventType.SNORING))
                .build();
            
            return Optional.of(insight);
        }

        return Optional.empty();
    }
} 