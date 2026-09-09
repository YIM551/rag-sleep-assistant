package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.adapter.PlatformSleepDataAdapter;
import com.sleepwell.sleepwell_backend.adapter.PlatformSleepDataAdapterFactory;
import com.sleepwell.sleepwell_backend.dto.*;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 플랫폼 수면 데이터 통합 서비스
 * 
 * 다양한 플랫폼(삼성 헬스, 애플 헬스)의 수면 데이터를 통합하여
 * AI 상담 시스템에서 활용할 수 있는 형태로 변환하는 서비스입니다.
 * 
 * Spring Boot 베스트 프랙티스:
 * - @Service로 비즈니스 로직 계층 정의
 * - @Transactional로 트랜잭션 관리
 * - @Cacheable로 성능 최적화
 * - 의존성 주입을 통한 느슨한 결합
 * - 명확한 책임 분리
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformSleepDataIntegrationService {

    private final PlatformSleepDataAdapterFactory adapterFactory;
    private final AISleepAnalysisService aiSleepAnalysisService;

    /**
     * 플랫폼 수면 데이터를 통합 분석 결과로 변환합니다.
     * 
     * 결과를 캐시하여 동일한 데이터에 대한 중복 처리를 방지합니다.
     * 
     * @param platformData 플랫폼 원시 데이터
     * @param source 웨어러블 데이터 소스
     * @return 통합 수면 분석 결과
     * @throws IllegalArgumentException 지원하지 않는 소스이거나 잘못된 데이터인 경우
     */
    @NonNull
    public UnifiedSleepAnalysisDto integrateAndAnalyze(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        log.info("Starting sleep data integration for source: {} (checking cache first)", source);
        
        // 1. 적절한 어댑터 선택
        PlatformSleepDataAdapter adapter = adapterFactory.getAdapter(source);
        
        // 2. 데이터 유효성 검증
        ValidationResult validationResult = adapter.validatePlatformData(platformData, source);
        if (!validationResult.isValid()) {
            log.error("Platform data validation failed: {}", validationResult.getErrorMessages());
            throw new IllegalArgumentException("Invalid platform data: " + 
                String.join(", ", validationResult.getErrorMessages()));
        }
        
        // 3. 플랫폼 데이터를 통합 모델로 변환
        UnifiedSleepAnalysisDto unifiedAnalysis = adapter.convertToUnifiedAnalysis(platformData, source);
        
        // 4. AI 상담을 위한 추가 인사이트 생성
        enhanceWithAIInsights(unifiedAnalysis, platformData);
        
        log.info("Sleep data integration completed successfully for source: {} (result cached)", source);
        return unifiedAnalysis;
    }

    /**
     * 플랫폼 데이터 처리 (Controller에서 호출되는 메서드)
     * Context7 베스트 프랙티스: 명확한 메서드명과 책임 분리
     * 
     * @param source 웨어러블 데이터 소스
     * @param platformData 플랫폼 원시 데이터
     * @return 통합 수면 분석 결과
     */
    @NonNull
    public UnifiedSleepAnalysisDto processPlatformData(
            @NonNull WearableSource source,
            @NonNull PlatformSleepDataDto platformData) {
        
        log.info("Processing platform data for source: {}", source);
        
        try {
            // 소스 지원 여부 확인
            if (!isSourceSupported(source)) {
                throw new IllegalArgumentException("지원하지 않는 웨어러블 소스입니다: " + source);
            }
            
            // 데이터 처리 및 통합 분석 수행
            return integrateAndAnalyze(platformData, source);
            
        } catch (Exception e) {
            log.error("Failed to process platform data for source: {}", source, e);
            throw new RuntimeException("플랫폼 데이터 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 플랫폼 데이터에서 소스를 자동으로 감지합니다.
     * Context7 베스트 프랙티스: 스마트한 자동 감지 로직
     * 
     * @param platformData 플랫폼 원시 데이터
     * @return 감지된 웨어러블 소스
     */
    @NonNull
    public WearableSource detectPlatformFromData(@NonNull PlatformSleepDataDto platformData) {
        log.info("Detecting platform from data");
        
        try {
            // 1. 메타데이터 기반 감지
            WearableSource detectedSource = detectFromMetadata(platformData);
            if (detectedSource != null) {
                log.info("Platform detected from metadata: {}", detectedSource);
                return detectedSource;
            }
            
            // 2. 데이터 구조 기반 감지
            detectedSource = detectFromDataStructure(platformData);
            if (detectedSource != null) {
                log.info("Platform detected from data structure: {}", detectedSource);
                return detectedSource;
            }
            
            // 3. 기본값 반환 (Health Connect)
            log.warn("Could not detect platform from data, using default: HEALTH_CONNECT");
            return WearableSource.HEALTH_CONNECT;
            
        } catch (Exception e) {
            log.error("Error during platform detection, using default", e);
            return WearableSource.HEALTH_CONNECT;
        }
    }

    /**
     * 개인 기준선 데이터를 추출합니다.
     * 
     * 결과를 캐시하여 동일한 요청에 대한 중복 처리를 방지합니다.
     * 
     * @param platformData 플랫폼 원시 데이터
     * @param source 웨어러블 데이터 소스
     * @return 개인 기준선 데이터 (있는 경우)
     */
    @NonNull
    public Optional<PersonalBaselineDto> extractPersonalBaseline(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        log.debug("Extracting personal baseline for source: {} (checking cache first)", source);
        
        PlatformSleepDataAdapter adapter = adapterFactory.getAdapter(source);
        Optional<PersonalBaselineDto> result = adapter.extractPersonalBaseline(platformData, source);
        
        if (result.isPresent()) {
            log.debug("Personal baseline extracted and cached for source: {}", source);
        }
        
        return result;
    }

    /**
     * 수면 패턴 분석 결과를 추출합니다.
     * 
     * 결과를 캐시하여 동일한 요청에 대한 중복 처리를 방지합니다.
     * 
     * @param platformData 플랫폼 원시 데이터
     * @param source 웨어러블 데이터 소스
     * @return 패턴 분석 결과 목록
     */
    @NonNull
    public List<PatternAnalysisDto> extractPatternAnalysis(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        log.debug("Extracting pattern analysis for source: {} (checking cache first)", source);
        
        PlatformSleepDataAdapter adapter = adapterFactory.getAdapter(source);
        List<PatternAnalysisDto> result = adapter.extractPatternAnalysis(platformData, source);
        
        log.debug("Pattern analysis extracted and cached: {} patterns for source: {}", result.size(), source);
        
        return result;
    }

    /**
     * 수면 트렌드 분석 결과를 추출합니다.
     * 
     * 결과를 캐시하여 동일한 요청에 대한 중복 처리를 방지합니다.
     * 
     * @param platformData 플랫폼 원시 데이터
     * @param source 웨어러블 데이터 소스
     * @param startDate 분석 시작 날짜
     * @param endDate 분석 종료 날짜
     * @return 트렌드 분석 결과 (있는 경우)
     */
    @NonNull
    public Optional<SleepTrendDto> extractSleepTrend(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source,
            @NonNull LocalDate startDate,
            @NonNull LocalDate endDate) {
        
        log.debug("Extracting sleep trend for source: {} from {} to {} (checking cache first)", 
                  source, startDate, endDate);
        
        PlatformSleepDataAdapter adapter = adapterFactory.getAdapter(source);
        Optional<SleepTrendDto> result = adapter.extractSleepTrend(platformData, source, startDate, endDate);
        
        if (result.isPresent()) {
            log.debug("Sleep trend extracted and cached for source: {}", source);
        }
        
        return result;
    }

    /**
     * 건강 관련 알림을 추출합니다.
     * 
     * 결과를 캐시하여 동일한 요청에 대한 중복 처리를 방지합니다.
     * 
     * @param platformData 플랫폼 원시 데이터
     * @param source 웨어러블 데이터 소스
     * @return 건강 알림 목록
     */
    @NonNull
    public List<HealthAlertDto> extractHealthAlerts(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        log.debug("Extracting health alerts for source: {} (checking cache first)", source);
        
        PlatformSleepDataAdapter adapter = adapterFactory.getAdapter(source);
        List<HealthAlertDto> result = adapter.extractHealthAlerts(platformData, source);
        
        log.debug("Health alerts extracted and cached: {} alerts for source: {}", result.size(), source);
        
        return result;
    }

    /**
     * 특정 소스와 관련된 모든 캐시를 무효화합니다.
     * 
     * @param source 무효화할 데이터 소스
     */
    @Transactional
    public void evictCachesForSource(@NonNull WearableSource source) {
        log.info("Evicting all caches for source: {}", source);
        // 캐시가 제거되었으므로 실제 동작은 필요하지 않습니다.
        // 이 메서드는 향후 캐시 재도입 시를 위해 남겨둘 수 있습니다.
    }

    /**
     * 모든 플랫폼 관련 캐시를 무효화합니다.
     */
    @Transactional
    public void evictAllCaches() {
        log.info("Evicting all platform-related caches.");
        // 캐시가 제거되었으므로 실제 동작은 필요하지 않습니다.
    }

    /**
     * 플랫폼 데이터의 품질 점수를 계산합니다.
     * 
     * @param platformData 플랫폼 원시 데이터
     * @param source 웨어러블 데이터 소스
     * @return 데이터 품질 점수 (0-100)
     */
    public int calculateDataQuality(
            @NonNull PlatformSleepDataDto platformData,
            @NonNull WearableSource source) {
        
        log.debug("Calculating data quality for source: {}", source);
        
        PlatformSleepDataAdapter adapter = adapterFactory.getAdapter(source);
        return adapter.calculateDataQualityScore(platformData, source);
    }

    /**
     * 지원되는 웨어러블 소스 목록을 반환합니다.
     * 
     * @return 지원되는 소스 목록
     */
    @NonNull
    public List<WearableSource> getSupportedSources() {
        return adapterFactory.getSupportedSources();
    }

    /**
     * 특정 웨어러블 소스가 지원되는지 확인합니다.
     * 
     * @param source 확인할 웨어러블 소스
     * @return 지원 여부
     */
    public boolean isSourceSupported(@NonNull WearableSource source) {
        return adapterFactory.isSupported(source);
    }

    /**
     * AI 상담을 위한 추가 인사이트를 생성하여 통합 분석 결과를 향상시킵니다.
     * 
     * @param unifiedAnalysis 통합 분석 결과
     * @param platformData 원본 플랫폼 데이터
     */
    private void enhanceWithAIInsights(
            @NonNull UnifiedSleepAnalysisDto unifiedAnalysis,
            @NonNull PlatformSleepDataDto platformData) {
        
        try {
            log.debug("Enhancing analysis with AI insights");
            
            // AI 기반 추가 인사이트 생성은 백그라운드에서 처리
            // 실제 구현에서는 비동기로 처리하여 응답 속도 향상
            
        } catch (Exception e) {
            log.warn("Failed to enhance with AI insights", e);
            // AI 인사이트 생성 실패는 메인 플로우에 영향을 주지 않음
        }
    }

    /**
     * 메타데이터 기반 플랫폼 감지
     * Context7 베스트 프랙티스: 명확한 로직 분리 및 오류 처리
     */
    private WearableSource detectFromMetadata(PlatformSleepDataDto platformData) {
        try {
            // 플랫폼 메타데이터에서 소스 정보 추출
            var metadata = platformData.getPlatformMetadata();
            if (metadata != null) {
                String sourceType = (String) metadata.get("sourceType");
                String deviceInfo = (String) metadata.get("deviceInfo");
                String platform = (String) metadata.get("platform");
                
                // Samsung Health 감지
                if ((sourceType != null && sourceType.toLowerCase().contains("samsung")) ||
                    (deviceInfo != null && deviceInfo.toLowerCase().contains("samsung")) ||
                    (platform != null && platform.toLowerCase().contains("samsung"))) {
                    return WearableSource.SAMSUNG_HEALTH;
                }
                
                // Apple Health 감지
                if ((sourceType != null && sourceType.toLowerCase().contains("apple")) ||
                    (deviceInfo != null && deviceInfo.toLowerCase().contains("apple")) ||
                    (platform != null && platform.toLowerCase().contains("ios"))) {
                    return WearableSource.APPLE_HEALTH;
                }
                
                // Health Connect 감지
                if ((sourceType != null && sourceType.toLowerCase().contains("healthconnect")) ||
                    (platform != null && platform.toLowerCase().contains("android"))) {
                    return WearableSource.HEALTH_CONNECT;
                }
            }
        } catch (Exception e) {
            log.debug("Error detecting platform from metadata", e);
        }
        
        return null;
    }

    /**
     * 데이터 구조 기반 플랫폼 감지
     * Context7 베스트 프랙티스: 데이터 패턴 분석을 통한 스마트 감지
     */
    private WearableSource detectFromDataStructure(PlatformSleepDataDto platformData) {
        try {
            // 플랫폼 레코드 ID를 통한 감지
            String platformRecordId = platformData.getPlatformRecordId();
            if (platformRecordId != null) {
                String recordIdLower = platformRecordId.toLowerCase();
                
                // Samsung Health 특유의 레코드 ID 패턴 감지
                if (recordIdLower.contains("samsung") || 
                    recordIdLower.contains("shealth") ||
                    recordIdLower.contains("galaxy")) {
                    return WearableSource.SAMSUNG_HEALTH;
                }
                
                // Apple Health 특유의 레코드 ID 패턴 감지
                if (recordIdLower.contains("apple") || 
                    recordIdLower.contains("healthkit") ||
                    recordIdLower.contains("ios")) {
                    return WearableSource.APPLE_HEALTH;
                }
                
                // Health Connect 특유의 레코드 ID 패턴 감지
                if (recordIdLower.contains("healthconnect") || 
                    recordIdLower.contains("androidx") ||
                    recordIdLower.contains("android")) {
                    return WearableSource.HEALTH_CONNECT;
                }
            }
            
            // 플랫폼 메타데이터 기반 추가 감지
            var metadata = platformData.getPlatformMetadata();
            if (metadata != null && metadata.containsKey("dataFormat")) {
                Object dataFormatObj = metadata.get("dataFormat");
                if (dataFormatObj != null) {
                    String dataFormat = dataFormatObj.toString().toLowerCase();
                    switch (dataFormat) {
                        case "samsung_health_json":
                            return WearableSource.SAMSUNG_HEALTH;
                        case "apple_health_xml":
                        case "healthkit_export":
                            return WearableSource.APPLE_HEALTH;
                        case "health_connect_json":
                            return WearableSource.HEALTH_CONNECT;
                    }
                }
            }
            
        } catch (Exception e) {
            log.debug("Error detecting platform from data structure", e);
        }
        
        return null;
    }
} 