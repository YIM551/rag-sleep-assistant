package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.WearableSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 플랫폼에서 수집된 원시 수면 데이터 DTO
 * 
 * 삼성 헬스, 애플 헬스 등 다양한 플랫폼에서 수집된
 * 수면 관련 데이터를 담는 데이터 전송 객체입니다.
 * 
 * Spring Boot 베스트 프랙티스:
 * - Lombok을 활용한 보일러플레이트 코드 제거
 * - Builder 패턴으로 객체 생성 편의성 제공
 * - Jakarta Validation으로 데이터 유효성 검증
 * - 명확한 필드명과 문서화
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSleepDataDto {

    /**
     * 데이터 소스 (삼성 헬스, 애플 헬스 등)
     */
    @NotNull(message = "데이터 소스는 필수입니다")
    private WearableSource source;

    /**
     * 수면 시작 시간
     */
    @NotNull(message = "수면 시작 시간은 필수입니다")
    @PastOrPresent(message = "수면 시작 시간은 현재 또는 과거여야 합니다")
    private LocalDateTime sleepStartTime;

    /**
     * 수면 종료 시간
     */
    @NotNull(message = "수면 종료 시간은 필수입니다")
    @PastOrPresent(message = "수면 종료 시간은 현재 또는 과거여야 합니다")
    private LocalDateTime sleepEndTime;

    /**
     * 총 수면 시간 (분)
     */
    @PositiveOrZero(message = "총 수면 시간은 0 이상이어야 합니다")
    private Integer totalSleepMinutes;

    /**
     * 깊은 수면 시간 (분)
     */
    @PositiveOrZero(message = "깊은 수면 시간은 0 이상이어야 합니다")
    private Integer deepSleepMinutes;

    /**
     * 얕은 수면 시간 (분)
     */
    @PositiveOrZero(message = "얕은 수면 시간은 0 이상이어야 합니다")
    private Integer lightSleepMinutes;

    /**
     * REM 수면 시간 (분)
     */
    @PositiveOrZero(message = "REM 수면 시간은 0 이상이어야 합니다")
    private Integer remSleepMinutes;

    /**
     * 수면 중 각성 시간 (분)
     */
    @PositiveOrZero(message = "각성 시간은 0 이상이어야 합니다")
    private Integer awakeMinutes;

    /**
     * 침대에 있던 총 시간 (분)
     */
    @PositiveOrZero(message = "침대 시간은 0 이상이어야 합니다")
    private Integer timeInBedMinutes;

    /**
     * 수면 효율성 (%)
     * 플랫폼에서 계산된 값
     */
    @PositiveOrZero(message = "수면 효율성은 0 이상이어야 합니다")
    private Double sleepEfficiency;

    /**
     * 플랫폼에서 제공하는 수면 점수 (0-100)
     */
    @PositiveOrZero(message = "수면 점수는 0 이상이어야 합니다")
    private Integer platformSleepScore;

    /**
     * 각성 횟수
     */
    @PositiveOrZero(message = "각성 횟수는 0 이상이어야 합니다")
    private Integer wakeupCount;

    /**
     * 평균 심박수 (BPM)
     */
    @PositiveOrZero(message = "심박수는 0 이상이어야 합니다")
    private Double averageHeartRate;

    /**
     * 평균 호흡수 (분당)
     */
    @PositiveOrZero(message = "호흡수는 0 이상이어야 합니다")
    private Double averageRespiratoryRate;

    /**
     * 평균 혈중 산소 포화도 (%)
     */
    @PositiveOrZero(message = "산소 포화도는 0 이상이어야 합니다")
    private Double averageSpO2;

    /**
     * 평균 체온 (섭씨)
     */
    private Double averageBodyTemperature;

    /**
     * 플랫폼 고유 식별자
     */
    private String platformRecordId;

    /**
     * 개인 기준선 데이터 (30일 평균 등)
     * JSON 형태로 저장
     */
    private Map<String, Object> personalBaseline;

    /**
     * AI 패턴 분석 결과
     * JSON 형태로 저장
     */
    private Map<String, Object> aiPatternAnalysis;

    /**
     * 건강 알림 데이터
     * (수면 무호흡, 호흡 장애 등)
     */
    private Map<String, Object> healthAlerts;

    /**
     * 수면 트렌드 데이터
     * (주간, 월간 패턴 등)
     */
    private Map<String, Object> sleepTrends;

    /**
     * 플랫폼별 추가 메타데이터
     */
    private Map<String, Object> platformMetadata;

    /**
     * 데이터 수집 시간
     */
    @NotNull(message = "데이터 수집 시간은 필수입니다")
    private LocalDateTime collectedAt;

    /**
     * 데이터 품질 점수 (0-100)
     * 플랫폼에서 제공하는 데이터의 완성도
     */
    @PositiveOrZero(message = "데이터 품질 점수는 0 이상이어야 합니다")
    private Integer dataQualityScore;
} 