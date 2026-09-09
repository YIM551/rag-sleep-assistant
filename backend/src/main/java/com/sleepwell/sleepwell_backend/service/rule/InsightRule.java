package com.sleepwell.sleepwell_backend.service.rule;

import com.sleepwell.sleepwell_backend.dto.InsightDto;
import com.sleepwell.sleepwell_backend.dto.SleepAudioTrendAnalysisDto;

import java.util.Optional;

/**
 * 인사이트 생성 규칙 인터페이스 (전략 패턴의 전략 역할)
 * 
 * 모든 구체적인 인사이트 생성 규칙은 이 인터페이스를 구현해야 합니다.
 * 각 규칙은 독립적으로 적용 가능 여부를 판단하고, 해당하는 경우 인사이트를 생성합니다.
 */
public interface InsightRule {

    /**
     * 현재 분석 데이터에 이 규칙을 적용할 수 있는지 확인합니다.
     *
     * @param analysisData 트렌드 분석 데이터
     * @return 규칙 적용이 가능하면 true, 아니면 false
     */
    boolean supports(SleepAudioTrendAnalysisDto analysisData);

    /**
     * 분석 데이터를 기반으로 인사이트를 생성합니다.
     * 이 메서드는 supports()가 true일 때만 호출되어야 합니다.
     *
     * @param analysisData 트렌드 분석 데이터
     * @return 생성된 인사이트. 생성할 인사이트가 없으면 Optional.empty()
     */
    Optional<InsightDto> generate(SleepAudioTrendAnalysisDto analysisData);
} 