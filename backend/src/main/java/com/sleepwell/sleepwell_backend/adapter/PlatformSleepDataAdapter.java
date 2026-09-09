package com.sleepwell.sleepwell_backend.adapter;

import com.sleepwell.sleepwell_backend.dto.*;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 플랫폼별 수면 데이터를 통합 모델로 변환하는 어댑터 인터페이스
 * 
 * 삼성 헬스와 애플 헬스에서 제공하는 개인화된 수면 분석 데이터를
 * 우리 시스템의 통합 데이터 모델로 변환하는 역할을 담당합니다.
 * 
 * Spring Boot 베스트 프랙티스:
 * - 인터페이스 기반 설계로 확장성 보장
 * - @NonNull, @Nullable 어노테이션으로 null 안전성 제공
 * - Optional 사용으로 명시적인 null 처리
 * - 명확한 메서드 명명 규칙 적용
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
public interface PlatformSleepDataAdapter {

    /**
     * 플랫폼별 원시 수면 데이터를 통합 분석 데이터로 변환
     * 
     * @param platformData 플랫폼에서 수집된 원시 수면 데이터
     * @param source 데이터 소스 (삼성 헬스, 애플 헬스 등)
     * @return 통합된 수면 분석 데이터
     * @throws IllegalArgumentException 지원하지 않는 플랫폼 소스인 경우
     */
    @NonNull
    UnifiedSleepAnalysisDto convertToUnifiedAnalysis(
            @NonNull PlatformSleepDataDto platformData, 
            @NonNull WearableSource source
    );

    /**
     * 플랫폼에서 제공하는 개인화된 기준선 데이터 추출
     * 
     * @param platformData 플랫폼 데이터
     * @param source 데이터 소스
     * @return 개인 기준선 데이터 (없을 경우 Optional.empty())
     */
    @NonNull
    Optional<PersonalBaselineDto> extractPersonalBaseline(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source
    );

    /**
     * 플랫폼에서 제공하는 AI 패턴 분석 결과 추출
     * 
     * @param platformData 플랫폼 데이터
     * @param source 데이터 소스
     * @return AI 패턴 분석 결과 리스트
     */
    @NonNull
    List<PatternAnalysisDto> extractPatternAnalysis(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source
    );

    /**
     * 특정 기간의 수면 트렌드 데이터 추출
     * 
     * @param platformData 플랫폼 데이터
     * @param source 데이터 소스
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 수면 트렌드 데이터
     */
    @NonNull
    Optional<SleepTrendDto> extractSleepTrend(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source,
            @NonNull LocalDate startDate,
            @NonNull LocalDate endDate
    );

    /**
     * 플랫폼에서 제공하는 건강 알림 데이터 추출
     * (수면 무호흡, 호흡 장애 등)
     * 
     * @param platformData 플랫폼 데이터
     * @param source 데이터 소스
     * @return 건강 알림 데이터 리스트
     */
    @NonNull
    List<HealthAlertDto> extractHealthAlerts(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source
    );

    /**
     * 해당 어댑터가 지원하는 플랫폼 소스인지 확인
     * 
     * @param source 확인할 데이터 소스
     * @return 지원 여부
     */
    boolean supports(@NonNull WearableSource source);

    /**
     * 플랫폼 데이터의 유효성 검증
     * 
     * @param platformData 검증할 플랫폼 데이터
     * @param source 데이터 소스
     * @return 유효성 검증 결과
     */
    @NonNull
    ValidationResult validatePlatformData(
            @Nullable PlatformSleepDataDto platformData,
            @NonNull WearableSource source
    );

    /**
     * 데이터 품질 점수 계산
     * 플랫폼에서 제공하는 데이터의 완성도와 신뢰도를 평가
     * 
     * @param platformData 평가할 플랫폼 데이터
     * @param source 데이터 소스
     * @return 0-100 범위의 품질 점수
     */
    int calculateDataQualityScore(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source
    );
} 