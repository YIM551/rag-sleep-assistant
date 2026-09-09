package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.AudioEventStatisticsDto;
import com.sleepwell.sleepwell_backend.dto.SleepAudioTrendAnalysisDto;
import com.sleepwell.sleepwell_backend.dto.SleepTrendAnalysisWithInsightsDto;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import com.sleepwell.sleepwell_backend.security.CustomUserDetails;
import com.sleepwell.sleepwell_backend.service.AudioEventStatisticsService;
import com.sleepwell.sleepwell_backend.service.SleepAudioTrendAnalysisService;
import com.sleepwell.sleepwell_backend.service.SleepQualityScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.time.DateTimeException;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 수면 분석 대시보드 API 컨트롤러
 * 
 * 연구 기반 수면 품질 점수 및 종합적인 수면 분석 데이터를 제공합니다.
 * 프론트엔드 대시보드 화면에서 사용할 수 있는 차트 데이터와 인사이트를 제공합니다.
 * 오디오 이벤트 분석 통합 대시보드 기능을 포함합니다.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "데이터 시각화", description = "수면 분석 대시보드 및 데이터 시각화 API")
@SecurityRequirement(name = "bearerAuth")
public class SleepAnalysisDashboardController {

    private final SleepQualityScoreService sleepQualityScoreService;
    private final SleepRecordRepository sleepRecordRepository;
    private final AudioEventStatisticsService audioEventStatisticsService;
    private final SleepAudioTrendAnalysisService sleepAudioTrendAnalysisService;
    private final com.sleepwell.sleepwell_backend.repository.UserRepository userRepository;

    /**
     * 일일 수면 분석 데이터 조회
     * 
     * 특정 날짜의 상세한 수면 분석 데이터를 제공합니다.
     * 수면 품질 점수, 수면 단계별 분석, 환경 분석, 방해 요소 분석 등을 포함합니다.
     * 
     * @param userDetails 인증된 사용자 정보
     * @param date 조회할 날짜 (yyyy-MM-dd 형식)
     * @return 일일 수면 분석 결과 (품질 점수, 인사이트, 수면 단계, 환경 데이터 포함)
     */
    @Operation(summary = "일일 수면 분석 데이터 조회",
               description = "특정 날짜의 상세한 수면 분석 데이터를 제공합니다. " +
                           "수면 품질 점수, 수면 단계별 분석, 환경 분석, 방해 요소 분석 등을 포함합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "일일 수면 분석 데이터 조회 성공",
                     content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> getDailySleepAnalysis(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd 형식)", required = true, example = "2024-01-15")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        
        User user = userDetails.getUser();
        
        // 미래 날짜 검증 (오늘 날짜는 허용)
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("미래 날짜의 수면 데이터는 조회할 수 없습니다.");
        }

        // 해당 날짜의 수면 기록 조회
        Optional<SleepRecord> sleepRecordOpt = sleepRecordRepository.findByUserAndDate(user, date);
        
        if (sleepRecordOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "해당 날짜의 수면 데이터가 없습니다.");
            response.put("hasData", false);
            response.put("date", date);
            return ResponseEntity.ok(response);
        }
        
        SleepRecord sleepRecord = sleepRecordOpt.get();
        
        // 가중치 기반 수면 품질 점수 계산 또는 기존 점수 사용
        Integer qualityScore = sleepRecord.getSleepQualityScore();
        if (qualityScore == null) {
            qualityScore = sleepQualityScoreService.calculateWeightedSleepQualityScore(sleepRecord);
        }
        
        // 수면 품질 인사이트 생성
        Map<String, Object> insights = sleepQualityScoreService.generateSleepQualityInsights(sleepRecord);
        
        // 수면 단계 분석
        Map<String, Object> sleepStages = calculateSleepStageAnalysis(sleepRecord);
        if (sleepStages == null) {
            sleepStages = new HashMap<>();
        }
        
        // 환경 분석
        Map<String, Object> environmentAnalysis = calculateEnvironmentAnalysis(sleepRecord);
        if (environmentAnalysis == null) {
            environmentAnalysis = new HashMap<>();
        }
        
        // 방해 요소 분석
        Map<String, Object> disruptionAnalysis = calculateDisruptionAnalysis(sleepRecord);
        if (disruptionAnalysis == null) {
            disruptionAnalysis = new HashMap<>();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        response.put("hasData", true);
        response.put("qualityScore", qualityScore);
        response.put("insights", insights);
        response.put("sleepStages", sleepStages);

        Map<String, Object> sleepTime = new HashMap<>();
        sleepTime.put("bedTime", sleepRecord.getSleepStartTime());
        sleepTime.put("wakeTime", sleepRecord.getSleepEndTime());
        sleepTime.put("totalMinutes", sleepRecord.getTotalSleepMinutes());
        sleepTime.put("efficiency", sleepRecord.calculateSleepEfficiency());
        response.put("sleepTime", sleepTime);

        response.put("environment", environmentAnalysis);
        response.put("disruptions", disruptionAnalysis);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 주간 수면 분석 데이터 조회
     * 
     * 시작 날짜부터 7일간의 수면 분석 데이터를 제공합니다.
     * 일별 데이터, 주간 통계, 개인화된 권장사항을 포함합니다.
     * 
     * @param userDetails 인증된 사용자 정보
     * @param startDate 시작 날짜 (yyyy-MM-dd 형식)
     * @return 주간 수면 분석 결과 (7일간의 일별 데이터, 주간 통계, 권장사항 포함)
     */
    @Operation(summary = "주간 수면 분석 데이터 조회",
               description = "시작 날짜부터 7일간의 수면 분석 데이터를 제공합니다. " +
                           "일별 데이터, 주간 통계, 개인화된 권장사항을 포함합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "주간 수면 분석 데이터 조회 성공",
                     content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/weekly")
    public ResponseEntity<Map<String, Object>> getWeeklySleepAnalysis(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "주간 분석 시작 날짜 (yyyy-MM-dd 형식)", required = true, example = "2024-01-15")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        
        User user = userDetails.getUser();

            LocalDate endDate = startDate.plusDays(6);
            List<SleepRecord> weeklyRecords = sleepRecordRepository
                .findByDateRange(user, startDate, endDate);
            
            // 날짜별 데이터 매핑
            Map<LocalDate, SleepRecord> recordMap = weeklyRecords.stream()
                .collect(Collectors.toMap(SleepRecord::getRecordDate, record -> record));
            
            List<Map<String, Object>> dailyData = new ArrayList<>();
            List<Integer> qualityScores = new ArrayList<>();
            List<Integer> sleepDurations = new ArrayList<>();
            
            for (int i = 0; i < 7; i++) {
                LocalDate currentDate = startDate.plusDays(i);
                SleepRecord record = recordMap.get(currentDate);
                
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                dayData.put("dayOfWeek", currentDate.getDayOfWeek().name());
                
                if (record != null) {
                    Integer score = record.getSleepQualityScore();
                    if (score == null) {
                        score = sleepQualityScoreService.calculateWeightedSleepQualityScore(record);
                    }
                    
                    dayData.put("hasData", true);
                    dayData.put("qualityScore", score);
                    dayData.put("sleepDuration", record.getTotalSleepMinutes());
                    dayData.put("sleepEfficiency", record.calculateSleepEfficiency());
                    dayData.put("deepSleepRatio", record.calculateDeepSleepRatio());
                    dayData.put("remSleepRatio", record.calculateRemSleepRatio());
                    
                    qualityScores.add(score);
                    sleepDurations.add(record.getTotalSleepMinutes());
                } else {
                    dayData.put("hasData", false);
                }
                
                dailyData.add(dayData);
            }
            
            // 주간 통계 계산
            Map<String, Object> weeklyStats = calculateWeeklyStats(qualityScores, sleepDurations);
            
            Map<String, Object> response = new HashMap<>();
            response.put("period", Map.of(
                "startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                "endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ));
            response.put("dailyData", dailyData);
            response.put("weeklyStats", weeklyStats);
            response.put("recommendations", generateWeeklyRecommendations(weeklyStats));
            
            return ResponseEntity.ok(response);
    }

    /**
     * 월간 수면 분석 데이터 조회
     * 
     * 지정된 연도와 월의 전체 수면 분석 데이터를 제공합니다.
     * 월간 통계, 성취도, 개선 권장사항을 포함합니다.
     * 
     * @param userDetails 인증된 사용자 정보
     * @param year 연도 (예: 2024)
     * @param month 월 (1-12)
     * @return 월간 수면 분석 결과 (월간 통계, 성취도, 권장사항 포함)
     */
    @Operation(summary = "월간 수면 분석 데이터 조회",
               description = "지정된 연도와 월의 전체 수면 분석 데이터를 제공합니다. " +
                           "월간 통계, 성취도, 개선 권장사항을 포함합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "월간 수면 분석 데이터 조회 성공",
                     content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 연도 또는 월 파라미터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlySleepAnalysis(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "조회할 연도", required = true, example = "2024")
            @RequestParam int year,
            @Parameter(description = "조회할 월 (1-12)", required = true, example = "1")
            @RequestParam int month) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        
        User user = userDetails.getUser();
        
        LocalDate startDate;
        try {
            startDate = LocalDate.of(year, month, 1);
        } catch (DateTimeException e) {
            log.error("월간 수면 분석 조회 중 잘못된 날짜 파라미터: {}", e.getMessage());
            throw new BusinessException("잘못된 연도 또는 월 파라미터입니다: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        List<SleepRecord> monthlyRecords = sleepRecordRepository.findByDateRange(user, startDate, endDate);
            
            Map<String, Object> monthlyStats = calculateMonthlyStats(monthlyRecords);
            
            Map<String, Object> response = new HashMap<>();
            response.put("period", Map.of(
                "year", year,
                "month", month,
                "startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                "endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ));
            response.put("monthlyStats", monthlyStats);
            response.put("achievements", generateMonthlyAchievements(monthlyStats));
            response.put("recommendations", generateMonthlyRecommendations(monthlyStats));
            
            return ResponseEntity.ok(response);
    }

    /**
     * 수면 패턴 분석 조회
     * 
     * 지정된 기간 동안의 수면 패턴을 종합적으로 분석합니다.
     * 요일별 패턴, 수면 시간대 패턴, 수면 품질 패턴, 개인 기준선 대비 분석을 포함합니다.
     * 
     * @param userDetails 인증된 사용자 정보
     * @param days 분석할 기간 (일수, 기본값: 30일)
     * @return 수면 패턴 분석 결과 (요일별, 시간대별, 품질별 패턴 및 인사이트 포함)
     */
    @Operation(summary = "수면 패턴 분석 조회",
               description = "지정된 기간 동안의 수면 패턴을 종합적으로 분석합니다. " +
                           "요일별 패턴, 수면 시간대 패턴, 수면 품질 패턴, 개인 기준선 대비 분석을 포함합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수면 패턴 분석 조회 성공",
                     content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 기간 파라미터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/patterns")
    public ResponseEntity<Map<String, Object>> getSleepPatterns(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "분석할 기간 (일수)", required = false, example = "30")
            @RequestParam(defaultValue = "30") int days) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        
        User user = userDetails.getUser();

        if (days <= 0) {
            throw new BusinessException("분석 기간은 1일 이상이어야 합니다", HttpStatus.BAD_REQUEST);
        }
        if (days > 365) {
            throw new BusinessException("분석 기간은 365일을 초과할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days - 1);
        List<SleepRecord> records = sleepRecordRepository.findByDateRange(user, startDate, endDate);
            
        if (records.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "message", "분석할 수면 데이터가 없습니다.",
                "period", Map.of(
                    "startDate", startDate.toString(),
                    "endDate", endDate.toString(),
                    "days", days
                )
            ));
        }

            Map<String, Object> weekdayPattern = analyzeWeekdayPattern(records);
        Map<String, Object> sleepTimePattern = analyzeSleepTimePattern(records);
            Map<String, Object> qualityPattern = analyzeSleepQualityPattern(records);
            Map<String, Object> baselineComparison = analyzeBaselineComparison(user, records);
            
            Map<String, Object> response = new HashMap<>();
        response.put("period", Map.of(
            "startDate", startDate.toString(),
            "endDate", endDate.toString(),
            "days", days
            ));
            response.put("weekdayPattern", weekdayPattern);
        response.put("sleepTimePattern", sleepTimePattern);
            response.put("qualityPattern", qualityPattern);
            response.put("baselineComparison", baselineComparison);
        response.put("insights", generatePatternInsights(weekdayPattern, sleepTimePattern, qualityPattern));
            
            return ResponseEntity.ok(response);
    }

    // === 헬퍼 메서드들 ===

    /**
     * 수면 단계별 분석 데이터 계산
     * 
     * @param record 수면 기록
     * @return 수면 단계별 시간과 비율 정보
     */
    private Map<String, Object> calculateSleepStageAnalysis(SleepRecord record) {
        Map<String, Object> stages = new HashMap<>();
        
        Integer totalMinutes = record.getTotalSleepMinutes();
        if (totalMinutes != null && totalMinutes > 0) {
            stages.put("deep", Map.of(
                "minutes", record.getDeepSleepMinutes() != null ? record.getDeepSleepMinutes() : 0,
                "ratio", record.calculateDeepSleepRatio() != null ? record.calculateDeepSleepRatio() : 0.0
            ));
            stages.put("light", Map.of(
                "minutes", record.getLightSleepMinutes() != null ? record.getLightSleepMinutes() : 0,
                "ratio", record.calculateLightSleepRatio() != null ? record.calculateLightSleepRatio() : 0.0
            ));
            stages.put("rem", Map.of(
                "minutes", record.getRemSleepMinutes() != null ? record.getRemSleepMinutes() : 0,
                "ratio", record.calculateRemSleepRatio() != null ? record.calculateRemSleepRatio() : 0.0
            ));
        }
        
        return stages;
    }

    /**
     * 수면 환경 분석 데이터 계산
     * 
     * @param record 수면 기록
     * @return 온도, 습도, 조도, 소음 레벨 정보
     */
    private Map<String, Object> calculateEnvironmentAnalysis(SleepRecord record) {
        return Map.of(
            "temperature", record.getTemperature() != null ? record.getTemperature() : "측정 안됨",
            "humidity", record.getHumidity() != null ? record.getHumidity() : "측정 안됨",
            "lightLevel", record.getLightLevel() != null ? record.getLightLevel() : "측정 안됨",
            "noiseLevel", record.getNoiseLevel() != null ? record.getNoiseLevel() : "측정 안됨"
        );
    }

    /**
     * 수면 방해 요소 분석 데이터 계산
     * 
     * @param record 수면 기록
     * @return 코골이, 이갈이, 잠꼬대, 깨어남 횟수 정보
     */
    private Map<String, Object> calculateDisruptionAnalysis(SleepRecord record) {
        return Map.of(
            "snoring", record.getSnoreDetected() != null ? record.getSnoreDetected() : false,
            "bruxism", record.getBruxismDetected() != null ? record.getBruxismDetected() : false,
            "sleepTalk", record.getSleepTalkDetected() != null ? record.getSleepTalkDetected() : false,
            "wakeupCount", record.getWakeupCount() != null ? record.getWakeupCount() : 0
        );
    }

    /**
     * 주간 통계 계산
     * 
     * @param qualityScores 주간 수면 품질 점수 목록
     * @param sleepDurations 주간 수면 시간 목록
     * @return 평균 품질 점수, 평균 수면 시간, 일관성 등의 주간 통계
     */
    private Map<String, Object> calculateWeeklyStats(List<Integer> qualityScores, List<Integer> sleepDurations) {
        if (qualityScores.isEmpty()) {
            return Map.of("hasData", false);
        }
        
        double avgQuality = qualityScores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double avgDuration = sleepDurations.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        
        return Map.of(
            "hasData", true,
            "averageQualityScore", Math.round(avgQuality),
            "averageSleepDuration", Math.round(avgDuration),
            "daysWithData", qualityScores.size(),
            "consistency", calculateConsistency(sleepDurations)
        );
    }

    /**
     * 월간 통계 계산
     * 
     * @param records 월간 수면 기록 목록
     * @return 평균 품질 점수, 평균 수면 시간, 최고/최저 점수 등의 월간 통계
     */
    private Map<String, Object> calculateMonthlyStats(List<SleepRecord> records) {
        if (records.isEmpty()) {
            return Map.of("hasData", false);
        }
        
        List<Integer> scores = records.stream()
            .map(r -> r.getSleepQualityScore() != null ? r.getSleepQualityScore() : 
                     sleepQualityScoreService.calculateWeightedSleepQualityScore(r))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        double avgScore = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double avgDuration = records.stream()
            .mapToInt(r -> r.getTotalSleepMinutes() != null ? r.getTotalSleepMinutes() : 0)
            .average().orElse(0.0);
        
        return Map.of(
            "hasData", true,
            "totalRecords", records.size(),
            "averageQualityScore", Math.round(avgScore),
            "averageSleepDuration", Math.round(avgDuration),
            "bestScore", scores.stream().mapToInt(Integer::intValue).max().orElse(0),
            "worstScore", scores.stream().mapToInt(Integer::intValue).min().orElse(0)
        );
    }

    /**
     * 주간 권장사항 생성
     * 
     * @param stats 주간 통계 데이터
     * @return 개인화된 주간 권장사항 목록
     */
    private List<String> generateWeeklyRecommendations(Map<String, Object> stats) {
        List<String> recommendations = new ArrayList<>();
        
        Boolean hasData = (Boolean) stats.get("hasData");
        if (hasData != null && hasData) {
            // Number 타입으로 받아서 안전하게 변환
            Number avgQualityNum = (Number) stats.get("averageQualityScore");
            if (avgQualityNum != null && avgQualityNum.intValue() < 70) {
                recommendations.add("주간 평균 수면 품질이 낮습니다. 수면 환경과 습관을 점검해보세요.");
            }
            
            Number consistencyNum = (Number) stats.get("consistency");
            if (consistencyNum != null && consistencyNum.doubleValue() < 0.8) {
                recommendations.add("수면 시간의 일관성을 높이기 위해 규칙적인 취침 시간을 유지하세요.");
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("좋은 수면 패턴을 유지하고 계십니다!");
        }
        
        return recommendations;
    }

    /**
     * 월간 성취도 생성
     * 
     * @param stats 월간 통계 데이터
     * @return 월간 성취도 목록
     */
    private List<String> generateMonthlyAchievements(Map<String, Object> stats) {
        List<String> achievements = new ArrayList<>();
        
        // Number 타입으로 받아서 안전하게 변환
        Number avgScoreNum = (Number) stats.get("averageQualityScore");
        if (avgScoreNum != null && avgScoreNum.intValue() >= 80) {
            achievements.add("우수한 월간 수면 품질 달성!");
        }
        
        Number totalRecordsNum = (Number) stats.get("totalRecords");
        if (totalRecordsNum != null && totalRecordsNum.intValue() >= 25) {
            achievements.add("꾸준한 수면 기록 관리!");
        }
        
        return achievements;
    }

    /**
     * 월간 권장사항 생성
     * 
     * @param stats 월간 통계 데이터
     * @return 월간 권장사항 목록
     */
    private List<String> generateMonthlyRecommendations(Map<String, Object> stats) {
        return List.of(
            "다음 달에는 수면 일관성에 더 집중해보세요.",
            "주말과 평일의 수면 패턴 차이를 줄여보세요."
        );
    }

    /**
     * 수면 일관성 점수 계산
     * 
     * @param values 수면 시간 또는 품질 점수 목록
     * @return 일관성 점수 (0-1, 높을수록 일관적)
     */
    private double calculateConsistency(List<Integer> values) {
        if (values.size() < 2) return 1.0;
        
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        // 일관성 점수 (0-1): 표준편차가 낮을수록 높은 점수
        return Math.max(0, 1 - (stdDev / mean));
    }

    /**
     * 요일별 수면 패턴 분석
     * 
     * @param records 수면 기록 목록
     * @return 평일/주말 평균, 가장 일관적인 요일 등의 분석 결과
     */
    private Map<String, Object> analyzeWeekdayPattern(List<SleepRecord> records) {
        // 요일별 패턴 분석
        return Map.of(
            "weekdayAverage", "평균 계산 필요",
            "weekendAverage", "평균 계산 필요",
            "mostConsistentDay", "분석 필요"
        );
    }

    /**
     * 수면 시간대 패턴 분석
     * 
     * @param records 수면 기록 목록
     * @return 취침 시간, 기상 시간 패턴 분석 결과
     */
    private Map<String, Object> analyzeSleepTimePattern(List<SleepRecord> records) {
        // 수면 시간대 패턴 분석
        return Map.of(
            "averageBedtime", "계산 필요",
            "averageWakeTime", "계산 필요",
            "bedtimeConsistency", "계산 필요"
        );
    }

    /**
     * 수면 품질 패턴 분석
     * 
     * @param records 수면 기록 목록
     * @return 수면 품질 변화 추세 및 패턴 분석 결과
     */
    private Map<String, Object> analyzeSleepQualityPattern(List<SleepRecord> records) {
        // 수면 품질 패턴 분석
        return Map.of(
            "qualityTrend", "분석 필요",
            "bestQualityConditions", "분석 필요"
        );
    }

    /**
     * 개인 기준선 대비 분석
     * 
     * @param user 사용자 정보
     * @param records 수면 기록 목록
     * @return 개인 평균 대비 현재 수면 패턴 비교 분석 결과
     */
    private Map<String, Object> analyzeBaselineComparison(User user, List<SleepRecord> records) {
        // 개인 기준선 대비 분석
        return Map.of(
            "comparedToBaseline", "분석 필요",
            "improvement", "분석 필요"
        );
    }

    /**
     * 패턴 분석 인사이트 생성
     * 
     * @param weekday 요일별 패턴 분석 결과
     * @param time 시간대 패턴 분석 결과
     * @param quality 품질 패턴 분석 결과
     * @return 종합적인 패턴 인사이트 목록
     */
    private List<String> generatePatternInsights(Map<String, Object> weekday, Map<String, Object> time, Map<String, Object> quality) {
        return List.of(
            "패턴 분석 결과를 바탕으로 한 인사이트가 여기에 표시됩니다.",
            "개인화된 수면 개선 제안사항이 제공됩니다."
        );
    }

    // ==================== 오디오 이벤트 분석 통합 대시보드 API ====================

    /**
     * 오디오 이벤트 분석 통합 대시보드 데이터 조회
     * 
     * 사용자 대시보드에 필요한 모든 오디오 이벤트 분석 데이터를 단일 요청으로 제공합니다.
     * 성능 최적화를 위해 필요한 데이터만 집계하여 반환합니다.
     */
    @GetMapping("/audio-events/dashboard")
    @Operation(summary = "오디오 이벤트 분석 통합 대시보드 조회",
               description = "사용자 대시보드에 필요한 모든 오디오 이벤트 분석 데이터를 단일 요청으로 제공합니다. " +
                           "최근 통계, 트렌드 분석, 인사이트를 포함합니다.")
    @ApiResponse(responseCode = "200", description = "대시보드 데이터 조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    public ResponseEntity<Map<String, Object>> getAudioEventDashboard(
            @Parameter(description = "분석 기간 (일수, 기본값: 30일)")
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        
        log.info("Getting audio event dashboard for user: {} with {} days period", 
                authentication.getName(), days);
        
        try {
            Long userId = extractUserIdFromAuthentication(authentication);
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days);
            
            // 1. 비동기적으로 분석 데이터 수집
            DashboardAnalysisData analysisData = collectAnalysisDataAsync(userId, startDate, endDate);
            
            // 2. 대시보드 응답 구성
            Map<String, Object> dashboard = buildDashboardResponse(analysisData, startDate, endDate, days);
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            log.error("Audio event dashboard retrieval failed for user: {}", authentication.getName(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "대시보드 데이터 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 비동기적으로 분석 데이터를 수집합니다.
     * 
     * 이 메서드는 여러 분석 작업을 병렬로 실행하여 성능을 최적화합니다.
     * CompletableFuture를 사용하여 다음 작업들을 동시에 수행합니다:
     * 1. 전체 기간 통계 계산
     * 2. 트렌드 분석 수행
     * 3. 최근 7일 통계 계산
     */
    /**
     * 비동기적으로 대시보드 분석 데이터 수집
     * 
     * 여러 분석 작업을 병렬로 실행하여 성능을 최적화합니다.
     * 
     * @param userId 사용자 ID
     * @param startDate 분석 시작 날짜
     * @param endDate 분석 종료 날짜
     * @return 수집된 분석 데이터 (통계, 트렌드, 최근 통계)
     * @throws ExecutionException 비동기 작업 실행 중 오류
     * @throws InterruptedException 비동기 작업 중단
     */
    private DashboardAnalysisData collectAnalysisDataAsync(Long userId, LocalDate startDate, LocalDate endDate) 
            throws ExecutionException, InterruptedException {
        
        // 1. 전체 기간 통계 계산 (비동기)
        CompletableFuture<AudioEventStatisticsDto> statisticsFuture = CompletableFuture
                .supplyAsync(() -> audioEventStatisticsService.calculateStatistics(userId, startDate, endDate));
        
        // 2. 트렌드 분석 수행 (비동기)
        CompletableFuture<SleepTrendAnalysisWithInsightsDto> trendFuture = CompletableFuture
                .supplyAsync(() -> performTrendAnalysis(userId, startDate, endDate));
        
        // 3. 최근 7일 통계 계산 (비동기)
        CompletableFuture<AudioEventStatisticsDto> recentStatsFuture = CompletableFuture
                .supplyAsync(() -> audioEventStatisticsService.calculateStatistics(userId, endDate.minusDays(7), endDate));
        
        // 4. 모든 비동기 작업 완료 대기
        CompletableFuture.allOf(statisticsFuture, trendFuture, recentStatsFuture).join();
        
        // 5. 결과 수집 및 반환
        return DashboardAnalysisData.builder()
                .statistics(statisticsFuture.get())
                .trendAnalysis(trendFuture.get())
                .recentStats(recentStatsFuture.get())
                .build();
    }

    /**
     * 트렌드 분석을 수행합니다.
     * 
     * SleepAudioTrendAnalysisService를 사용하여 트렌드를 분석하고,
     * 빈 인사이트 리스트와 함께 래핑하여 반환합니다.
     */
    /**
     * 수면 트렌드 분석 수행
     * 
     * @param userId 사용자 ID
     * @param startDate 분석 시작 날짜
     * @param endDate 분석 종료 날짜
     * @return 트렌드 분석 결과 (인사이트 포함)
     */
    private SleepTrendAnalysisWithInsightsDto performTrendAnalysis(Long userId, LocalDate startDate, LocalDate endDate) {
        SleepAudioTrendAnalysisDto trendAnalysis = sleepAudioTrendAnalysisService.analyzeTrend(userId, startDate, endDate);
        return new SleepTrendAnalysisWithInsightsDto(trendAnalysis, new ArrayList<>());
    }

    /**
     * 대시보드 응답을 구성합니다.
     * 
     * 수집된 분석 데이터를 기반으로 사용자에게 제공할 대시보드 응답을 구성합니다.
     */
    /**
     * 대시보드 응답 데이터 구성
     * 
     * @param analysisData 수집된 분석 데이터
     * @param startDate 분석 시작 날짜
     * @param endDate 분석 종료 날짜
     * @param days 분석 기간 (일수)
     * @return 구조화된 대시보드 응답 데이터
     */
    private Map<String, Object> buildDashboardResponse(DashboardAnalysisData analysisData, 
                                                      LocalDate startDate, LocalDate endDate, int days) {
        Map<String, Object> dashboard = new HashMap<>();
        
        // 기본 정보 추가
        dashboard.put("period", buildPeriodInfo(startDate, endDate, days));
        
        // 핵심 지표 요약 추가
        dashboard.put("summary", buildSummaryInfo(analysisData.getStatistics()));
        
        // 최근 비교 데이터 추가
        dashboard.put("recentComparison", buildRecentComparison(analysisData.getStatistics(), analysisData.getRecentStats()));
        
        // 이벤트 분포 정보 추가
        dashboard.put("eventDistribution", buildEventDistribution(analysisData.getStatistics()));
        
        // 트렌드 분석 정보 추가
        dashboard.put("trends", buildTrendsInfo(analysisData.getStatistics(), analysisData.getTrendAnalysis()));
        
        // 주요 인사이트 추가
        dashboard.put("keyInsights", extractKeyInsights(analysisData.getTrendAnalysis()));
        
        // 권장사항 추가
        dashboard.put("recommendations", extractRecommendations(analysisData.getStatistics()));
        
        // 상태 정보 추가
        dashboard.put("status", buildStatusInfo(analysisData.getStatistics(), analysisData.getRecentStats()));
        
        return dashboard;
    }

    /**
     * 기간 정보를 구성합니다.
     */
    /**
     * 분석 기간 정보 구성
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param days 분석 기간 (일수)
     * @return 기간 정보 맵
     */
    private Map<String, Object> buildPeriodInfo(LocalDate startDate, LocalDate endDate, int days) {
        return Map.of(
            "startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            "endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            "days", days
        );
    }

    /**
     * 핵심 지표 요약 정보를 구성합니다.
     */
    /**
     * 요약 정보 구성
     * 
     * @param statistics 오디오 이벤트 통계
     * @return 요약 정보 맵 (총 이벤트 수, 평균 일일 이벤트 등)
     */
    private Map<String, Object> buildSummaryInfo(AudioEventStatisticsDto statistics) {
        return Map.of(
            "totalEvents", statistics.getTotalAudioEvents(),
            "averageDailyEvents", statistics.getAverageDailyEvents(),
            "averageIntensity", statistics.getAverageIntensity(),
            "highIntensityRatio", statistics.getHighIntensityEventRatio(),
            "medicalAttentionRatio", statistics.getMedicalAttentionRequiredRatio()
        );
    }

    /**
     * 최근 7일 vs 전체 기간 비교 정보를 구성합니다.
     */
    /**
     * 최근 기간 비교 정보 구성
     * 
     * @param statistics 전체 기간 통계
     * @param recentStats 최근 7일 통계
     * @return 비교 정보 맵 (변화율, 개선/악화 여부 등)
     */
    private Map<String, Object> buildRecentComparison(AudioEventStatisticsDto statistics, AudioEventStatisticsDto recentStats) {
        return Map.of(
            "recent7Days", Map.of(
                "totalEvents", recentStats.getTotalAudioEvents(),
                "averageIntensity", recentStats.getAverageIntensity()
            ),
            "fullPeriod", Map.of(
                "totalEvents", statistics.getTotalAudioEvents(),
                "averageIntensity", statistics.getAverageIntensity()
            )
        );
    }

    /**
     * 이벤트 유형별 분포 정보를 구성합니다.
     */
    /**
     * 이벤트 분포 정보 구성
     * 
     * @param statistics 오디오 이벤트 통계
     * @return 이벤트 유형별 분포 정보 맵
     */
    private Map<String, Object> buildEventDistribution(AudioEventStatisticsDto statistics) {
        return Map.of(
            "byType", statistics.getEventCountByType(),
            "byIntensity", statistics.getAverageIntensityByType(),
            "byRatio", statistics.getEventRatioByType()
        );
    }

    /**
     * 트렌드 분석 정보를 구성합니다.
     */
    /**
     * 트렌드 정보 구성
     * 
     * @param statistics 오디오 이벤트 통계
     * @param trendAnalysis 트렌드 분석 결과
     * @return 트렌드 정보 맵 (방향성, 인사이트 등)
     */
    private Map<String, Object> buildTrendsInfo(AudioEventStatisticsDto statistics, SleepTrendAnalysisWithInsightsDto trendAnalysis) {
        return Map.of(
            "weeklyTrends", statistics.getWeeklyTrends(),
            "overallTrend", trendAnalysis.getTrendAnalysis().getTrendIndicators().getOverallTrendDirection(),
            "riskLevel", statistics.getOverallRiskLevel()
        );
    }

    /**
     * 주요 인사이트를 추출합니다 (최대 3개).
     */
    /**
     * 핵심 인사이트 추출
     * 
     * @param trendAnalysis 트렌드 분석 결과
     * @return 핵심 인사이트 목록
     */
    private List<String> extractKeyInsights(SleepTrendAnalysisWithInsightsDto trendAnalysis) {
        return trendAnalysis.getInsights().stream()
                .limit(3)
                .map(insight -> insight.getMessage())
                .collect(Collectors.toList());
    }

    /**
     * 권장사항을 추출합니다 (최대 3개).
     */
    /**
     * 권장사항 추출
     * 
     * @param statistics 오디오 이벤트 통계
     * @return 개인화된 권장사항 목록
     */
    private List<String> extractRecommendations(AudioEventStatisticsDto statistics) {
        return statistics.getKeyRecommendations().stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    /**
     * 상태 정보를 구성합니다.
     */
    /**
     * 상태 정보 구성
     * 
     * @param statistics 전체 기간 통계
     * @param recentStats 최근 통계
     * @return 상태 정보 맵 (건강 상태, 경고 등)
     */
    private Map<String, Object> buildStatusInfo(AudioEventStatisticsDto statistics, AudioEventStatisticsDto recentStats) {
        return Map.of(
            "lastUpdated", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            "dataQuality", statistics.getAverageConfidenceScore(),
            "hasRecentData", recentStats.getTotalAudioEvents() > 0
        );
    }

    /**
     * 대시보드 분석 데이터를 담는 내부 클래스
     * 
     * 비동기 작업의 결과를 구조화하여 관리하기 위한 데이터 클래스입니다.
     * Builder 패턴을 사용하여 객체 생성을 단순화합니다.
     */
    /**
     * 대시보드 분석 데이터 컨테이너
     * 
     * 비동기 작업으로 수집된 다양한 분석 결과를 담는 불변 객체입니다.
     * Builder 패턴을 사용하여 안전하게 객체를 생성합니다.
     */
    @Getter
    @Builder
    private static class DashboardAnalysisData {
        /** 전체 기간 오디오 이벤트 통계 */
        private final AudioEventStatisticsDto statistics;
        
        /** 수면 트렌드 분석 결과 (인사이트 포함) */
        private final SleepTrendAnalysisWithInsightsDto trendAnalysis;
        
        /** 최근 7일 오디오 이벤트 통계 */
        private final AudioEventStatisticsDto recentStats;
    }

    /**
     * 실시간 오디오 분석 상태 조회
     * 
     * 현재 진행 중인 오디오 분석 작업의 상태를 실시간으로 확인합니다.
     */
    @GetMapping("/audio-events/analysis-status")
    @Operation(summary = "실시간 오디오 분석 상태 조회",
               description = "현재 진행 중인 오디오 이벤트 분석 작업의 상태를 실시간으로 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "분석 상태 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getAudioAnalysisStatus(
            Authentication authentication) {
        
        log.info("Getting audio analysis status for user: {}", authentication.getName());
        
        try {
            Long userId = extractUserIdFromAuthentication(authentication);
            
            // 실제 구현에서는 분석 작업 큐나 상태 관리 서비스에서 조회
            Map<String, Object> status = new HashMap<>();
            
            // 현재 진행 중인 분석 작업들
            status.put("activeAnalyses", List.of(
                Map.of(
                    "id", "analysis-001",
                    "type", "AUDIO_EVENT_DETECTION",
                    "status", "IN_PROGRESS",
                    "progress", 75,
                    "startedAt", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    "estimatedCompletion", "2분 후"
                )
            ));
            
            // 최근 완료된 분석
            status.put("recentCompleted", List.of(
                Map.of(
                    "id", "analysis-002",
                    "type", "TREND_ANALYSIS",
                    "status", "COMPLETED",
                    "completedAt", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    "results", "새로운 패턴 감지됨"
                )
            ));
            
            // 시스템 상태
            status.put("systemStatus", Map.of(
                "analysisEngine", "HEALTHY",
                "queueSize", 3,
                "averageProcessingTime", "5분",
                "lastSystemCheck", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            ));
            
            // 사용자별 분석 설정
            status.put("userSettings", Map.of(
                "autoAnalysis", true,
                "sensitivity", "MEDIUM",
                "notificationsEnabled", true,
                "analysisFrequency", "DAILY"
            ));
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Audio analysis status retrieval failed for user: {}", authentication.getName(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "분석 상태 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 오디오 이벤트 분석 결과 내보내기
     * 
     * 지정된 기간의 분석 결과를 CSV 또는 JSON 형태로 내보냅니다.
     */
    @GetMapping("/audio-events/export")
    @Operation(summary = "오디오 이벤트 분석 결과 내보내기",
               description = "지정된 기간의 오디오 이벤트 분석 결과를 CSV 또는 JSON 형태로 내보냅니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "내보내기 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<byte[]> exportAudioEventAnalysis(
            @Parameter(description = "시작 날짜 (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "내보내기 형식 (csv, json)", required = false)
            @RequestParam(defaultValue = "csv") String format,
            @Parameter(description = "포함할 이벤트 유형들 (쉼표로 구분)", required = false)
            @RequestParam(required = false) List<AudioEventType> eventTypes,
            Authentication authentication) {
        
        log.info("Exporting audio event analysis for user: {} from {} to {} in {} format", 
                authentication.getName(), startDate, endDate, format);
        
        try {
            Long userId = extractUserIdFromAuthentication(authentication);
            
            // 날짜 범위 검증
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 이전이어야 합니다.");
            }
            
            // 최대 1년 제한
            if (startDate.plusYears(1).isBefore(endDate)) {
                throw new IllegalArgumentException("내보내기 가능한 최대 기간은 1년입니다.");
            }
            
            // 분석 데이터 조회
            AudioEventStatisticsDto statistics = audioEventStatisticsService
                    .calculateStatistics(userId, startDate, endDate);
            
            byte[] exportData;
            String filename;
            String contentType;
            
            if ("json".equalsIgnoreCase(format)) {
                // JSON 형태로 내보내기
                exportData = generateJsonExport(statistics);
                filename = String.format("audio_analysis_%s_%s.json", startDate, endDate);
                contentType = MediaType.APPLICATION_JSON_VALUE;
            } else {
                // CSV 형태로 내보내기 (기본값)
                exportData = generateCsvExport(statistics, eventTypes);
                filename = String.format("audio_analysis_%s_%s.csv", startDate, endDate);
                contentType = "text/csv";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(exportData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(exportData);
                    
        } catch (IllegalArgumentException e) {
            log.warn("Invalid export request for user: {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(e.getMessage().getBytes());
        } catch (Exception e) {
            log.error("Audio event analysis export failed for user: {}", authentication.getName(), e);
            return ResponseEntity.internalServerError()
                    .body("내보내기 중 오류가 발생했습니다.".getBytes());
        }
    }

    /**
     * 오디오 이벤트 분석 설정 조회
     */
    @GetMapping("/audio-events/settings")
    @Operation(summary = "오디오 이벤트 분석 설정 조회",
               description = "사용자의 오디오 이벤트 분석 관련 설정을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "설정 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> getAudioAnalysisSettings(
            Authentication authentication) {
        
        log.info("Getting audio analysis settings for user: {}", authentication.getName());
        
        try {
            Long userId = extractUserIdFromAuthentication(authentication);
            
            // 실제 구현에서는 사용자 설정 서비스에서 조회
            Map<String, Object> settings = new HashMap<>();
            
            settings.put("general", Map.of(
                "autoAnalysisEnabled", true,
                "analysisFrequency", "DAILY",
                "notificationsEnabled", true,
                "dataRetentionDays", 365
            ));
            
            settings.put("sensitivity", Map.of(
                "snoringDetection", "MEDIUM",
                "bruxismDetection", "HIGH",
                "sleepTalkDetection", "LOW",
                "environmentalNoiseDetection", "MEDIUM"
            ));
            
            settings.put("thresholds", Map.of(
                "highIntensityThreshold", 7,
                "medicalAttentionThreshold", 8,
                "alertFrequencyLimit", 3
            ));
            
            settings.put("privacy", Map.of(
                "dataSharing", false,
                "anonymousAnalytics", true,
                "exportEnabled", true
            ));
            
            return ResponseEntity.ok(settings);
            
        } catch (Exception e) {
            log.error("Audio analysis settings retrieval failed for user: {}", authentication.getName(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "설정 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 오디오 이벤트 분석 설정 업데이트
     */
    @PutMapping("/audio-events/settings")
    @Operation(summary = "오디오 이벤트 분석 설정 업데이트",
               description = "사용자의 오디오 이벤트 분석 관련 설정을 업데이트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "설정 업데이트 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 설정 값"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> updateAudioAnalysisSettings(
            @RequestBody Map<String, Object> settingsUpdate,
            Authentication authentication) {
        
        log.info("Updating audio analysis settings for user: {}", authentication.getName());
        
        try {
            Long userId = extractUserIdFromAuthentication(authentication);
            
            // 설정 유효성 검증
            validateSettingsUpdate(settingsUpdate);
            
            // 실제 구현에서는 사용자 설정 서비스에서 업데이트
            Map<String, Object> updatedSettings = new HashMap<>();
            updatedSettings.put("status", "SUCCESS");
            updatedSettings.put("message", "설정이 성공적으로 업데이트되었습니다.");
            updatedSettings.put("updatedAt", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            updatedSettings.put("updatedFields", settingsUpdate.keySet());
            
            return ResponseEntity.ok(updatedSettings);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid settings update for user: {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Audio analysis settings update failed for user: {}", authentication.getName(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "설정 업데이트 중 오류가 발생했습니다."));
        }
    }

    // ==================== 헬퍼 메서드들 ====================

    /**
     * 인증 정보에서 사용자 ID 추출
     */
    /**
     * 인증 정보에서 사용자 ID 추출
     * 
     * @param authentication Spring Security 인증 객체
     * @return 사용자 ID
     * @throws IllegalStateException 인증 정보가 유효하지 않은 경우
     */
    private Long extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        // UserDetails 기반 인증의 경우 사용자명으로 조회
        String username = authentication.getName();
        // 실제 구현에서는 UserRepository를 통해 조회
        return 1L; // 임시 값
    }

    /**
     * JSON 형태로 데이터 내보내기
     */
    /**
     * JSON 형식 내보내기 데이터 생성
     * 
     * @param statistics 오디오 이벤트 통계
     * @return JSON 형식의 바이트 배열
     * @throws IOException JSON 생성 중 오류 발생
     */
    private byte[] generateJsonExport(AudioEventStatisticsDto statistics) throws IOException {
        // 실제 구현에서는 ObjectMapper를 사용하여 JSON 생성
        String json = String.format("""
            {
                "exportDate": "%s",
                "period": {
                    "start": "%s",
                    "end": "%s"
                },
                "summary": {
                    "totalEvents": %d,
                    "averageIntensity": %s,
                    "averageDailyEvents": %s
                },
                "eventsByType": %s,
                "recommendations": %s
            }
            """, 
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            statistics.getPeriodStart().format(DateTimeFormatter.ISO_LOCAL_DATE),
            statistics.getPeriodEnd().format(DateTimeFormatter.ISO_LOCAL_DATE),
            statistics.getTotalAudioEvents(),
            statistics.getAverageIntensity(),
            statistics.getAverageDailyEvents(),
            statistics.getEventCountByType(),
            statistics.getKeyRecommendations()
        );
        
        return json.getBytes();
    }

    /**
     * CSV 형태로 데이터 내보내기
     */
    /**
     * CSV 형식 내보내기 데이터 생성
     * 
     * @param statistics 오디오 이벤트 통계
     * @param eventTypes 포함할 이벤트 유형 목록
     * @return CSV 형식의 바이트 배열
     * @throws IOException CSV 생성 중 오류 발생
     */
    private byte[] generateCsvExport(AudioEventStatisticsDto statistics, List<AudioEventType> eventTypes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // CSV 헤더
        String header = "Date,EventType,Count,AverageIntensity,AverageDuration\n";
        outputStream.write(header.getBytes());
        
        // 이벤트 유형별 데이터
        Map<AudioEventType, Long> eventCounts = statistics.getEventCountByType();
        Map<AudioEventType, BigDecimal> avgIntensities = statistics.getAverageIntensityByType();
        Map<AudioEventType, BigDecimal> avgDurations = statistics.getAverageDurationByType();
        
        for (AudioEventType eventType : AudioEventType.values()) {
            if (eventTypes == null || eventTypes.contains(eventType)) {
                String row = String.format("%s,%s,%d,%s,%s\n",
                    statistics.getPeriodStart().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    eventType.name(),
                    eventCounts.getOrDefault(eventType, 0L),
                    avgIntensities.getOrDefault(eventType, BigDecimal.ZERO),
                    avgDurations.getOrDefault(eventType, BigDecimal.ZERO)
                );
                outputStream.write(row.getBytes());
            }
        }
        
        return outputStream.toByteArray();
    }

    /**
     * 설정 업데이트 유효성 검증
     */
    /**
     * 설정 업데이트 데이터 검증
     * 
     * @param settingsUpdate 업데이트할 설정 데이터
     * @throws IllegalArgumentException 유효하지 않은 설정 값인 경우
     */
    private void validateSettingsUpdate(Map<String, Object> settingsUpdate) {
        // 실제 구현에서는 각 설정 항목의 유효성을 검증
        if (settingsUpdate.containsKey("highIntensityThreshold")) {
            Object threshold = settingsUpdate.get("highIntensityThreshold");
            if (!(threshold instanceof Number) || ((Number) threshold).intValue() < 1 || ((Number) threshold).intValue() > 10) {
                throw new IllegalArgumentException("고강도 임계값은 1-10 사이여야 합니다.");
            }
        }
        
        if (settingsUpdate.containsKey("dataRetentionDays")) {
            Object retention = settingsUpdate.get("dataRetentionDays");
            if (!(retention instanceof Number) || ((Number) retention).intValue() < 30) {
                throw new IllegalArgumentException("데이터 보존 기간은 최소 30일이어야 합니다.");
            }
        }
    }
} 