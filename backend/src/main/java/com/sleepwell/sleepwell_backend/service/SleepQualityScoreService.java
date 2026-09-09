package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.Gender;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

/**
 * 수면 품질 점수 산정 서비스
 * 
 * 연구 기반 가중치 알고리즘을 활용하여 수면의 질을 종합적으로 평가합니다.
 * 주요 생리적/행동적 지표, 개인화 요소, 연령/성별 보정을 통한 정확한 점수를 제공합니다.
 * 
 * 참고: 수면 과학 연구 기반의 가중치 기반 수면 품질 점수 산정 알고리즘 개발 보고서
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SleepQualityScoreService {

    private final SleepRecordRepository sleepRecordRepository;

    // === 가중치 상수 (연구 기반) ===
    
    /**
     * 위계적 가중치 부여
     * 총합: 1.0 (100%)
     */
    private static final double DEEP_SLEEP_WEIGHT = 0.25;      // 깊은 수면 - 신체 회복
    private static final double REM_SLEEP_WEIGHT = 0.20;       // REM 수면 - 기억 통합
    private static final double SLEEP_EFFICIENCY_WEIGHT = 0.20; // 수면 효율성 - 수면 연속성
    private static final double TOTAL_SLEEP_TIME_WEIGHT = 0.15; // 총 수면 시간 - 연령 조정
    private static final double WASO_WEIGHT = 0.10;            // 수면 중 각성 시간 - 수면 단편화
    private static final double SPO2_WEIGHT = 0.05;            // 혈중 산소 포화도 - 호흡 장애
    private static final double OTHER_FACTORS_WEIGHT = 0.05;   // 기타 지표 (심박수, 환경 등)

    // === 최적 수면 범위 (성인 기준) ===
    
    private static final int OPTIMAL_SLEEP_TIME_MIN = 7 * 60;   // 7시간 (분)
    private static final int OPTIMAL_SLEEP_TIME_MAX = 9 * 60;   // 9시간 (분)
    private static final double OPTIMAL_SLEEP_EFFICIENCY = 0.85; // 85%
    private static final double OPTIMAL_DEEP_SLEEP_RATIO = 0.20; // 20%
    private static final double OPTIMAL_REM_SLEEP_RATIO = 0.22;  // 22%
    private static final int MAX_OPTIMAL_WASO = 30;             // 30분

    /**
     * 연구 기반 가중치 수면 품질 점수 계산
     * 
     * @param sleepRecord 수면 기록
     * @return 수면 품질 점수 (0-100)
     */
    public Integer calculateWeightedSleepQualityScore(SleepRecord sleepRecord) {
        try {
            // Null 안전성 체크
            if (sleepRecord == null) {
                log.warn("수면 기록이 null입니다.");
                return null;
            }
            
            User user = sleepRecord.getUser();
            
            // 1. 개별 지표 점수 계산 (0-100)
            double deepSleepScore = calculateDeepSleepScore(sleepRecord);
            double remSleepScore = calculateRemSleepScore(sleepRecord);
            double sleepEfficiencyScore = calculateSleepEfficiencyScore(sleepRecord);
            double totalSleepTimeScore = calculateTotalSleepTimeScore(sleepRecord, user);
            double wasoScore = calculateWasoScore(sleepRecord);
            double spo2Score = calculateSpO2Score(sleepRecord);
            double otherFactorsScore = calculateOtherFactorsScore(sleepRecord);
            
            // 2. 가중치 적용 점수 계산
            double weightedScore = 
                deepSleepScore * DEEP_SLEEP_WEIGHT +
                remSleepScore * REM_SLEEP_WEIGHT +
                sleepEfficiencyScore * SLEEP_EFFICIENCY_WEIGHT +
                totalSleepTimeScore * TOTAL_SLEEP_TIME_WEIGHT +
                wasoScore * WASO_WEIGHT +
                spo2Score * SPO2_WEIGHT +
                otherFactorsScore * OTHER_FACTORS_WEIGHT;
            
            // 3. 개인화 조정 적용
            double personalizedScore = applyPersonalizationFactors(weightedScore, sleepRecord, user);
            
            // 4. 최종 점수 (0-100 범위로 제한)
            int finalScore = (int) Math.round(Math.max(0, Math.min(100, personalizedScore)));
            
            log.debug("수면 품질 점수 계산 완료 - 사용자: {}, 날짜: {}, 점수: {}", 
                     user != null ? user.getEmail() : "Unknown", sleepRecord.getRecordDate(), finalScore);
            
            return finalScore;
            
        } catch (Exception e) {
            log.error("수면 품질 점수 계산 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 깊은 수면 점수 계산
     * 깊은 수면은 신체 회복과 면역 기능에 가장 중요
     */
    private double calculateDeepSleepScore(SleepRecord sleepRecord) {
        Double deepSleepRatio = sleepRecord.calculateDeepSleepRatio();
        if (deepSleepRatio == null) {
            return 70.0; // 데이터 없을 경우 중간 점수
        }
        
        // 최적 범위: 15-25%, 20%가 이상적
        if (deepSleepRatio >= 0.15 && deepSleepRatio <= 0.25) {
            // 최적 범위 내에서는 20%에 가까울수록 높은 점수
            double distanceFromOptimal = Math.abs(deepSleepRatio - OPTIMAL_DEEP_SLEEP_RATIO);
            return Math.max(90, 100 - (distanceFromOptimal * 50)); // 최적 범위에서는 최소 90점, 더 관대한 감점
        } else if (deepSleepRatio < 0.15) {
            // 부족한 경우 (15% 미만)
            return Math.max(30, deepSleepRatio * 400); // 최소 30점
        } else {
            // 과도한 경우 (25% 초과) - 덜 심각한 문제
            return Math.max(60, 100 - (deepSleepRatio - 0.25) * 300);
        }
    }

    /**
     * REM 수면 점수 계산
     * REM 수면은 기억 통합과 감정 조절에 중요
     */
    private double calculateRemSleepScore(SleepRecord sleepRecord) {
        Double remSleepRatio = sleepRecord.calculateRemSleepRatio();
        if (remSleepRatio == null) {
            return 70.0;
        }
        
        // 최적 범위: 20-25%, 22%가 이상적
        if (remSleepRatio >= 0.20 && remSleepRatio <= 0.25) {
            double distanceFromOptimal = Math.abs(remSleepRatio - OPTIMAL_REM_SLEEP_RATIO);
            return Math.max(90, 100 - (distanceFromOptimal * 100)); // 최적 범위에서는 최소 90점, 더 관대한 감점
        } else if (remSleepRatio < 0.20) {
            return Math.max(40, remSleepRatio * 350);
        } else {
            return Math.max(70, 100 - (remSleepRatio - 0.25) * 200);
        }
    }

    /**
     * 수면 효율성 점수 계산
     * 수면 효율성은 불면증의 핵심 지표
     */
    private double calculateSleepEfficiencyScore(SleepRecord sleepRecord) {
        Double sleepEfficiency = sleepRecord.calculateSleepEfficiency();
        if (sleepEfficiency == null) {
            return 70.0;
        }
        
        double efficiencyRatio = sleepEfficiency / 100.0;
        
        if (efficiencyRatio >= OPTIMAL_SLEEP_EFFICIENCY) {
            // 85% 이상은 우수 - 94%는 매우 우수
            return Math.min(100, 90 + (efficiencyRatio - OPTIMAL_SLEEP_EFFICIENCY) * 67); // 94%일 때 96점
        } else if (efficiencyRatio >= 0.75) {
            // 75-85% 범위
            return 60 + (efficiencyRatio - 0.75) * 300; // 더 관대한 점수
        } else {
            // 75% 미만은 문제가 있음
            return Math.max(20, efficiencyRatio * 80);
        }
    }

    /**
     * 총 수면 시간 점수 계산 (연령 보정 적용)
     */
    private double calculateTotalSleepTimeScore(SleepRecord sleepRecord, User user) {
        Integer totalSleepMinutes = sleepRecord.getTotalSleepMinutes();
        if (totalSleepMinutes == null) {
            return 70.0;
        }
        
        // 연령별 최적 수면 시간 조정
        int[] optimalRange = getOptimalSleepTimeByAge(user);
        int optimalMin = optimalRange[0];
        int optimalMax = optimalRange[1];
        
        if (totalSleepMinutes >= optimalMin && totalSleepMinutes <= optimalMax) {
            return 100.0;
        } else if (totalSleepMinutes < optimalMin) {
            // 수면 부족
            double shortage = (double)(optimalMin - totalSleepMinutes) / optimalMin;
            return Math.max(30, 100 - shortage * 100);
        } else {
            // 과도한 수면 (상대적으로 덜 심각)
            double excess = (double)(totalSleepMinutes - optimalMax) / optimalMax;
            return Math.max(60, 100 - excess * 80);
        }
    }

    /**
     * WASO (Wake After Sleep Onset) 점수 계산
     * 수면 중 각성 시간 - 수면 단편화 지표
     */
    private double calculateWasoScore(SleepRecord sleepRecord) {
        Integer sleepAwakeMinutes = sleepRecord.getSleepAwakeMinutes();
        if (sleepAwakeMinutes == null) {
            return 80.0; // 데이터 없을 경우 양호한 점수로 가정
        }
        
        if (sleepAwakeMinutes <= MAX_OPTIMAL_WASO) {
            return 100.0;
        } else {
            // 30분 초과시 점수 감소
            double excessMinutes = sleepAwakeMinutes - MAX_OPTIMAL_WASO;
            return Math.max(20, 100 - excessMinutes * 1.5);
        }
    }

    /**
     * SpO2 (혈중 산소 포화도) 점수 계산
     * 수면 무호흡증 등 호흡 장애 지표
     */
    private double calculateSpO2Score(SleepRecord sleepRecord) {
        if (sleepRecord.getSpo2Data() == null || sleepRecord.getSpo2Data().isEmpty()) {
            return 85.0; // 기본값
        }
        
        try {
            // JSON 데이터에서 평균 SpO2 값 추출
            // 예: {"average": 97, "min": 94, "max": 99, "data_points": [...]}
            String spo2Data = sleepRecord.getSpo2Data();
            
            // 간단한 JSON 파싱 (실제로는 Jackson을 사용하는 것이 좋음)
            if (spo2Data.contains("\"average\":")) {
                int startIndex = spo2Data.indexOf("\"average\":") + 10;
                int endIndex = spo2Data.indexOf(",", startIndex);
                if (endIndex == -1) {
                    endIndex = spo2Data.indexOf("}", startIndex);
                }
                
                String avgStr = spo2Data.substring(startIndex, endIndex).trim();
                double avgSpO2 = Double.parseDouble(avgStr);
                
                // SpO2 정상 범위: 95-100%
                if (avgSpO2 >= 95) {
                    return 100.0; // 정상
                } else if (avgSpO2 >= 90) {
                    return 80.0 - (95 - avgSpO2) * 4; // 90-95%: 경도 저산소증
                } else if (avgSpO2 >= 85) {
                    return 40.0 - (90 - avgSpO2) * 4; // 85-90%: 중등도 저산소증
                } else {
                    return Math.max(0, 20.0 - (85 - avgSpO2) * 2); // 85% 미만: 중증 저산소증
                }
            }
        } catch (Exception e) {
            log.warn("SpO2 데이터 파싱 실패: {}", e.getMessage());
        }
        
        return 85.0; // 파싱 실패 시 기본값
    }

    /**
     * 기타 요인 점수 계산
     * 심박수, 환경 요인, 수면 방해 요소 등
     */
    private double calculateOtherFactorsScore(SleepRecord sleepRecord) {
        double score = 80.0; // 기본 점수
        
        // 수면 방해 요소 체크
        if (Boolean.TRUE.equals(sleepRecord.getSnoreDetected())) {
            score -= 15; // 코골이 감점
        }
        if (Boolean.TRUE.equals(sleepRecord.getBruxismDetected())) {
            score -= 10; // 이갈이 감점
        }
        if (Boolean.TRUE.equals(sleepRecord.getSleepTalkDetected())) {
            score -= 5; // 잠꼬대 감점
        }
        
        // 환경 요인 (온도, 습도 등)
        if (sleepRecord.getTemperature() != null) {
            double temp = sleepRecord.getTemperature();
            if (temp < 16 || temp > 24) { // 최적 온도 16-24°C
                score -= 5;
            }
        }
        
        return Math.max(30, score);
    }

    /**
     * 개인화 조정 적용
     * 연령, 성별, 개인 기준선 등을 고려
     */
    private double applyPersonalizationFactors(double baseScore, SleepRecord sleepRecord, User user) {
        double adjustedScore = baseScore;
        
        // User가 null인 경우 개인화 조정 없이 기본 점수 반환
        if (user == null) {
            return adjustedScore;
        }
        
        // 1. 연령 조정
        int age = calculateAge(user);
        if (age >= 65) {
            // 고령자는 수면 구조 변화로 조정
            adjustedScore += 5; // 관대한 평가
        } else if (age <= 25) {
            // 젊은층은 더 높은 기준 적용
            adjustedScore -= 2;
        }
        
        // 2. 성별 조정
        if (user.getGender() == Gender.FEMALE) {
            // 여성은 평균적으로 더 많은 깊은 수면
            Double deepSleepRatio = sleepRecord.calculateDeepSleepRatio();
            if (deepSleepRatio != null && deepSleepRatio > OPTIMAL_DEEP_SLEEP_RATIO) {
                adjustedScore += 2;
            }
        }
        
        // 3. 개인 기준선 조정 (최근 30일 평균 대비)
        Double personalBaseline = calculatePersonalBaseline(user);
        if (personalBaseline != null) {
            double deviation = baseScore - personalBaseline;
            // 개인 평균보다 크게 좋거나 나쁜 경우 약간 조정
            adjustedScore += deviation * 0.1;
        }
        
        return adjustedScore;
    }

    /**
     * 연령별 최적 수면 시간 범위 반환
     */
    private int[] getOptimalSleepTimeByAge(User user) {
        int age = calculateAge(user);
        
        if (age <= 17) {
            return new int[]{8 * 60, 10 * 60}; // 8-10시간
        } else if (age <= 64) {
            return new int[]{7 * 60, 9 * 60};  // 7-9시간
        } else {
            return new int[]{7 * 60, 8 * 60};  // 7-8시간
        }
    }

    /**
     * 나이 계산 (User 엔티티의 age 필드 사용)
     */
    private int calculateAge(User user) {
        if (user == null || user.getAge() == null) {
            return 30; // 기본값
        }
        return user.getAge();
    }

    /**
     * 개인 기준선 계산 (최근 30일 평균)
     */
    private Double calculatePersonalBaseline(User user) {
        if (user == null) return null;

        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(29); // 최근 30일

        List<SleepRecord> recentRecords = sleepRecordRepository.findByDateRange(user, startDate, endDate);

        if (recentRecords.size() < 7) { // 최소 7일 데이터 필요
            return null;
        }
        
        double totalScore = 0;
        int validRecords = 0;
        
        for (SleepRecord record : recentRecords) {
            if (record.getSleepQualityScore() != null) {
                totalScore += record.getSleepQualityScore();
                validRecords++;
            }
        }
        
        return validRecords > 0 ? totalScore / validRecords : null;
    }

    /**
     * 수면 품질 레벨 및 권장사항 생성
     */
    public Map<String, Object> generateSleepQualityInsights(SleepRecord sleepRecord) {
        Integer score = sleepRecord.getSleepQualityScore();
        if (score == null) {
            score = calculateWeightedSleepQualityScore(sleepRecord);
        }
        
        String level;
        String message;
        String recommendation;
        
        if (score >= 85) {
            level = "EXCELLENT";
            message = "우수한 수면 품질입니다!";
            recommendation = "현재의 수면 패턴을 유지하세요. 규칙적인 수면 시간과 환경을 계속 관리하시기 바랍니다.";
        } else if (score >= 70) {
            level = "GOOD";
            message = "양호한 수면 품질입니다.";
            recommendation = "수면 환경 개선과 취침 전 루틴 정비를 통해 더 나은 수면을 취할 수 있습니다.";
        } else if (score >= 50) {
            level = "FAIR";
            message = "보통 수준의 수면 품질입니다.";
            recommendation = "수면 시간 규칙화, 취침 전 스크린 시간 줄이기, 카페인 섭취 조절을 권장합니다.";
        } else {
            level = "POOR";
            message = "수면 품질 개선이 필요합니다.";
            recommendation = "지속적인 수면 문제가 있다면 수면 전문의 상담을 받아보시기 바랍니다. 수면 위생 관리를 철저히 하세요.";
        }
        
        return Map.of(
            "score", score,
            "level", level,
            "message", message,
            "recommendation", recommendation,
            "analysisDate", sleepRecord.getRecordDate()
        );
    }
} 