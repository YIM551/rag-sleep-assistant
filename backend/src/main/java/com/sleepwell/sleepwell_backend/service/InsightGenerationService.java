package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.InsightDto;
import com.sleepwell.sleepwell_backend.dto.SleepAudioTrendAnalysisDto;
import com.sleepwell.sleepwell_backend.service.rule.InsightRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 수면 분석 데이터로부터 인사이트를 생성하는 서비스 (서비스 로케이터 역할)
 * 
 * 주입된 모든 InsightRule 구현체들을 순회하며,
 * 현재 분석 데이터에 적용 가능한 모든 인사이트를 생성합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsightGenerationService {

    private final List<InsightRule> insightRules;

    /**
     * 분석 데이터를 기반으로 적용 가능한 모든 인사이트를 생성합니다.
     *
     * @param analysisData 수면 오디오 트렌드 분석 데이터
     * @return 생성된 인사이트 DTO 목록
     */
    public List<InsightDto> generateInsights(SleepAudioTrendAnalysisDto analysisData) {
        log.info("Generating insights based on analysis for user ID: {}", analysisData.getExternalAnalysisReference().getUserId());

        List<InsightDto> generatedInsights = insightRules.stream()
                .filter(rule -> {
                    boolean isSupported = rule.supports(analysisData);
                    log.debug("Rule [{}] support status: {}", rule.getClass().getSimpleName(), isSupported);
                    return isSupported;
                })
                .flatMap(rule -> rule.generate(analysisData).stream())
                .collect(Collectors.toList());

        log.info("Generated {} insights for user ID: {}", generatedInsights.size(), analysisData.getExternalAnalysisReference().getUserId());
        return generatedInsights;
    }
} 