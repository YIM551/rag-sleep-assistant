package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.SleepRecordRequestDto;
import com.sleepwell.sleepwell_backend.dto.SleepRecordResponseDto;
import com.sleepwell.sleepwell_backend.dto.UnifiedSleepAnalysisDto;
import com.sleepwell.sleepwell_backend.dto.UnifiedSleepAnalysisDto.SleepSessionInfo;
import com.sleepwell.sleepwell_backend.dto.UnifiedSleepAnalysisDto.SleepStageAnalysis;
import com.sleepwell.sleepwell_backend.dto.SleepAudioEventRequestDto;
import com.sleepwell.sleepwell_backend.dto.SleepAudioEventResponseDto;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.SleepAudioEvent;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 수면 기록 관리 서비스
 * 
 * 사용자의 수면 데이터 생명주기를 관리하는 핵심 비즈니스 로직을 담당합니다.
 * 다양한 웨어러블 디바이스와 플랫폼으로부터 수집된 수면 데이터를 
 * 통합하여 저장, 조회, 분석할 수 있는 기능을 제공합니다.
 * 
 * 주요 기능:
 * - 수면 기록 CRUD 작업
 * - 플랫폼 데이터 통합 및 변환
 * - 수면 품질 점수 계산
 * - 날짜/기간별 수면 데이터 조회
 * - 수면 패턴 분석을 위한 데이터 준비
 * 
 * 데이터 소스:
 * - Apple Health
 * - Samsung Health
 * - 수동 입력 데이터
 * - Flutter Health Plugin
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see SleepRecord
 * @see SleepQualityScoreService
 * @see PlatformSleepDataIntegrationService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SleepRecordService {

    private final SleepRecordRepository sleepRecordRepository;
    private final UserRepository userRepository;
    private final AISleepAnalysisService aiSleepAnalysisService;

    /**
     * 수면 기록 생성
     */
    @Transactional
    public SleepRecordResponseDto createSleepRecord(Long userId, SleepRecordRequestDto requestDto) {
        // 데이터 무결성 검증 추가
        requestDto.validateDataIntegrity();

        // 기존 로직 유지
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 동일 날짜 수면 기록 중복 방지
        LocalDate recordDate = requestDto.getSleepStartTime().toLocalDate();
        Optional<SleepRecord> existingRecord = sleepRecordRepository.findByUserAndRecordDate(user, recordDate);
        if (existingRecord.isPresent()) {
            throw new BusinessException("이미 해당 날짜에 수면 기록이 존재합니다.", HttpStatus.CONFLICT, "DUPLICATE_RECORD");
        }

        // 기존 로직 유지
        SleepRecord sleepRecord = SleepRecord.builder()
                .user(user)
                .recordDate(recordDate)
                .sleepStartTime(requestDto.getSleepStartTime())
                .sleepEndTime(requestDto.getSleepEndTime())
                .totalSleepMinutes(requestDto.getTotalSleepMinutes())
                // 다른 필드들도 동일하게 설정
                .build();

        // 기존 로직 유지
        SleepRecord savedRecord = sleepRecordRepository.save(sleepRecord);
        
        // AI 분석 비동기 트리거
        aiSleepAnalysisService.performAIAnalysis(savedRecord);

        return SleepRecordResponseDto.fromEntity(savedRecord);
    }

    /**
     * 플랫폼 통합 분석 데이터로 수면 기록 생성
     * Context7 베스트 프랙티스 적용: Spring Boot 표준 어노테이션, 트랜잭션 관리, 로깅
     */
    @Transactional
    public SleepRecordResponseDto createSleepRecordFromPlatformData(Long userId, UnifiedSleepAnalysisDto unifiedAnalysis) {
        log.info("Creating sleep record from platform data for user: {}", userId);

        User user = findUserById(userId);
        validatePlatformData(unifiedAnalysis);

        // 동일 날짜의 기존 기록 확인 (같은 날짜면 업데이트, 아니면 새로 생성)
        LocalDate recordDate = unifiedAnalysis.getSleepSession().getSleepTime().toLocalDate();
        Optional<SleepRecord> existingRecord = sleepRecordRepository.findByUserAndDate(user, recordDate);

        SleepRecord sleepRecord;
        if (existingRecord.isPresent()) {
            // 같은 날짜 기록이 있으면 업데이트 (플랫폼 재전송 허용)
            sleepRecord = updateExistingRecord(existingRecord.get(), unifiedAnalysis);
            log.info("Updated existing sleep record for date: {}", recordDate);
        } else {
            // 다른 날짜와 겹치는지만 검증 (중복 방지)
            validateOverlappingWithOtherDates(user, unifiedAnalysis, recordDate);
            sleepRecord = buildSleepRecordFromPlatformData(user, unifiedAnalysis);
            log.info("Created new sleep record for date: {}", recordDate);
        }
        
        sleepRecord = sleepRecordRepository.save(sleepRecord);
        
        if (shouldPerformAdditionalAiAnalysis(unifiedAnalysis)) {
            triggerAIAnalysis(sleepRecord);
        }
        
        return convertToResponseDto(sleepRecord);
    }

    /**
     * 사용자의 수면 기록 목록 조회 (페이징)
     */
    public Page<SleepRecordResponseDto> getSleepRecords(Long userId, int page, int size, String sortBy, String sortDir) {
        log.info("Getting sleep records for user: {}, page: {}, size: {}", userId, page, size);
        
        User user = findUserById(userId);

        // sleepDate -> recordDate로 변환 (하위 호환성)
        if ("sleepDate".equals(sortBy)) {
            sortBy = "recordDate";
            log.warn("sortBy 'sleepDate'는 deprecated입니다. 'recordDate'를 사용해주세요.");
        }
        
        // 유효한 필드명인지 검증
        if (!isValidSortField(sortBy)) {
            log.warn("Invalid sort field: {}. Using default 'recordDate'", sortBy);
            sortBy = "recordDate";
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<SleepRecord> sleepRecords = sleepRecordRepository.findAllByUser(user, pageable);
        
        return sleepRecords.map(this::convertToResponseDto);
    }
    
    /**
     * 유효한 정렬 필드명인지 검증
     */
    private boolean isValidSortField(String field) {
        return field != null && (
            "recordDate".equals(field) ||
            "createdAt".equals(field) ||
            "sleepStartTime".equals(field) ||
            "sleepEndTime".equals(field) ||
            "totalSleepMinutes".equals(field) ||
            "sleepQualityScore".equals(field)
        );
    }

    /**
     * 특정 수면 기록 조회
     */
    public SleepRecordResponseDto getSleepRecord(Long userId, Long recordId) {
        log.info("Getting sleep record: {} for user: {}", recordId, userId);
        
        SleepRecord sleepRecord = findSleepRecordByIdAndUserId(recordId, userId);
        return convertToResponseDto(sleepRecord);
    }

    /**
     * 특정 날짜의 수면 기록 조회
     */
    public SleepRecordResponseDto getSleepRecordByDate(Long userId, LocalDate date) {
        log.info("Getting sleep record for user: {} on date: {}", userId, date);
        
        User user = findUserById(userId);
        SleepRecord sleepRecord = sleepRecordRepository.findByUserAndDate(user, date)
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 수면 기록을 찾을 수 없습니다: " + date));

        return convertToResponseDto(sleepRecord);
    }

    /**
     * 기간별 수면 기록 조회 (페이징)
     */
    public Page<SleepRecordResponseDto> getSleepRecordsBetweenDates(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Getting sleep records for user: {} between {} and {} with paging", userId, startDate, endDate);
        
        User user = findUserById(userId);
        Page<SleepRecord> sleepRecords = sleepRecordRepository.findByDateRange(user, startDate, endDate, pageable);
        
        return sleepRecords.map(this::convertToResponseDto);
    }

    /**
     * 수면 기록 삭제
     */
    @Transactional
    public void deleteSleepRecord(Long userId, Long recordId) {
        log.info("Deleting sleep record: {} for user: {}", recordId, userId);
        
        SleepRecord sleepRecord = findSleepRecordByIdAndUserId(recordId, userId);
        sleepRecordRepository.delete(sleepRecord);
        log.info("Sleep record deleted successfully: {}", recordId);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    private SleepRecord findSleepRecordByIdAndUserId(Long recordId, Long userId) {
        SleepRecord sleepRecord = sleepRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("수면 기록을 찾을 수 없습니다: " + recordId, HttpStatus.NOT_FOUND, "RECORD_NOT_FOUND"));

        if (!sleepRecord.getUser().getId().equals(userId)) {
            throw new BusinessException("해당 수면 기록에 접근할 권한이 없습니다", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
        return sleepRecord;
    }

    private void triggerAIAnalysis(SleepRecord sleepRecord) {
        aiSleepAnalysisService.performAIAnalysis(sleepRecord)
            .thenAccept(analysis -> log.info("AI analysis completed for sleep record: {}", sleepRecord.getId()))
            .exceptionally(throwable -> {
                log.error("AI analysis failed for sleep record: {}", sleepRecord.getId(), throwable);
                return null;
            });
    }

    private SleepRecord buildSleepRecordFromRequest(User user, SleepRecordRequestDto requestDto) {
        return SleepRecord.builder()
                .user(user)
                .sleepStartTime(requestDto.getSleepStartTime())
                .sleepEndTime(requestDto.getSleepEndTime())
                .totalSleepMinutes(requestDto.getTotalSleepMinutes())
                .deepSleepMinutes(requestDto.getDeepSleepMinutes())
                .lightSleepMinutes(requestDto.getLightSleepMinutes())
                .remSleepMinutes(requestDto.getRemSleepMinutes())
                .wakeupCount(requestDto.getWakeupCount() != null ? requestDto.getWakeupCount() : 0)
                .sleepInBedMinutes(requestDto.getSleepInBedMinutes())
                .sleepAwakeMinutes(requestDto.getSleepAwakeMinutes())
                .lightLevel(requestDto.getLightLevel())
                .noiseLevel(requestDto.getNoiseLevel())
                .temperature(requestDto.getTemperature())
                .humidity(requestDto.getHumidity())
                .heartRateData(requestDto.getHeartRateData())
                .respiratoryRateData(requestDto.getRespiratoryRateData())
                .snoreDetected(requestDto.getSnoreDetected() != null ? requestDto.getSnoreDetected() : false)
                .bruxismDetected(requestDto.getBruxismDetected() != null ? requestDto.getBruxismDetected() : false)
                .sleepTalkDetected(requestDto.getSleepTalkDetected() != null ? requestDto.getSleepTalkDetected() : false)
                .audioDataPath(requestDto.getAudioDataPath())
                .wearableSource(requestDto.getWearableSource())
                .sleepQualityScore(requestDto.getSleepQualityScore())
                .userSatisfaction(requestDto.getUserSatisfaction())
                .recordDate(requestDto.getSleepStartTime().toLocalDate())
                .build();
    }

    private void validateSleepTimes(SleepRecordRequestDto requestDto) {
        LocalDateTime startTime = requestDto.getSleepStartTime();
        LocalDateTime endTime = requestDto.getSleepEndTime();
        
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("수면 시작 시간은 종료 시간보다 이전이어야 합니다.");
        }
    }

    private void validatePlatformData(UnifiedSleepAnalysisDto unifiedAnalysis) {
        if (unifiedAnalysis == null || unifiedAnalysis.getSleepSession() == null || unifiedAnalysis.getSleepSession().getSleepTime() == null || unifiedAnalysis.getSleepSession().getWakeTime() == null) {
            throw new IllegalArgumentException("플랫폼 데이터에 필수 정보(세션, 시작/종료 시간)가 누락되었습니다.");
        }
    }

    /**
     * 다른 날짜와 겹치는 시간대 검증 (같은 날짜 기록은 제외)
     * 수면 기록은 보통 하루에 하나만 존재하므로, 다른 날짜와 시간이 겹치면 데이터 오류
     */
    private void validateOverlappingWithOtherDates(User user, UnifiedSleepAnalysisDto unifiedAnalysis, LocalDate recordDate) {
        LocalDateTime newStartTime = unifiedAnalysis.getSleepSession().getSleepTime();
        LocalDateTime newEndTime = unifiedAnalysis.getSleepSession().getWakeTime();

        // 같은 사용자의 기존 수면 기록들 중에서 다른 날짜와 겹치는지 확인
        List<SleepRecord> existingRecords = sleepRecordRepository.findAllByUser(user);

        for (SleepRecord existingRecord : existingRecords) {
            // 같은 날짜는 스킵 (업데이트 대상이므로)
            if (existingRecord.getRecordDate().equals(recordDate)) {
                continue;
            }

            LocalDateTime existingStart = existingRecord.getSleepStartTime();
            LocalDateTime existingEnd = existingRecord.getSleepEndTime();

            // 다른 날짜와 겹치는 시간대 검증 (50% 이상 겹치는 경우만 오류)
            if (isSignificantOverlap(newStartTime, newEndTime, existingStart, existingEnd)) {
                throw new BusinessException(
                    String.format("다른 날짜(%s)의 수면 기록과 시간이 크게 겹칩니다.", existingRecord.getRecordDate()),
                    HttpStatus.CONFLICT,
                    "OVERLAPPING_TIME_RANGE"
                );
            }
        }
    }

    /**
     * 중복 검증 (deprecated - 더 이상 사용하지 않음)
     * 같은 날짜는 자동 업데이트하므로 이 메서드는 필요 없음
     */
    @Deprecated
    private void validateDuplicateAndOverlappingData(User user, UnifiedSleepAnalysisDto unifiedAnalysis) {
        // 이 메서드는 더 이상 사용되지 않음 - validateOverlappingWithOtherDates 사용
    }

    /**
     * 두 시간 범위가 50% 이상 겹치는지 확인 (의미있는 중복)
     */
    private boolean isSignificantOverlap(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        // 겹치는 부분이 없으면 false
        if (end1.isBefore(start2) || end1.equals(start2) || end2.isBefore(start1) || end2.equals(start1)) {
            return false;
        }

        // 겹치는 시간 계산
        LocalDateTime overlapStart = start1.isAfter(start2) ? start1 : start2;
        LocalDateTime overlapEnd = end1.isBefore(end2) ? end1 : end2;

        long overlapMinutes = java.time.Duration.between(overlapStart, overlapEnd).toMinutes();
        long totalMinutes = java.time.Duration.between(start1, end1).toMinutes();

        // 50% 이상 겹치면 true
        return overlapMinutes > 0 && (double) overlapMinutes / totalMinutes >= 0.5;
    }


    private SleepRecord buildSleepRecordFromPlatformData(User user, UnifiedSleepAnalysisDto unifiedAnalysis) {
        SleepSessionInfo sessionInfo = unifiedAnalysis.getSleepSession();
        SleepStageAnalysis stageAnalysis = unifiedAnalysis.getSleepStages();

        // 디버깅: SleepStageAnalysis 상태 로깅
        log.debug("Building SleepRecord from platform data - stageAnalysis is null: {}", stageAnalysis == null);
        if (stageAnalysis != null) {
            log.debug("StageAnalysis details - Deep: {}, Light: {}, REM: {}, Awake: {}",
                    stageAnalysis.getDeepSleepMinutes(),
                    stageAnalysis.getLightSleepMinutes(),
                    stageAnalysis.getRemSleepMinutes(),
                    stageAnalysis.getAwakeMinutes());
        }

        LocalDateTime startTime = sessionInfo.getSleepTime() != null ? sessionInfo.getSleepTime() : LocalDateTime.now().minusHours(8);

        return SleepRecord.builder()
                .user(user)
                .sleepStartTime(startTime)
                .sleepEndTime(sessionInfo.getWakeTime())
                .totalSleepMinutes(sessionInfo.getTotalSleepTime())
                .deepSleepMinutes(stageAnalysis != null && stageAnalysis.getDeepSleepMinutes() != null ? stageAnalysis.getDeepSleepMinutes() : 0)
                .lightSleepMinutes(stageAnalysis != null && stageAnalysis.getLightSleepMinutes() != null ? stageAnalysis.getLightSleepMinutes() : 0)
                .remSleepMinutes(stageAnalysis != null && stageAnalysis.getRemSleepMinutes() != null ? stageAnalysis.getRemSleepMinutes() : 0)
                .sleepAwakeMinutes(stageAnalysis != null && stageAnalysis.getAwakeMinutes() != null ? stageAnalysis.getAwakeMinutes() : null)
                .wakeupCount(sessionInfo.getWakeupCount() != null ? sessionInfo.getWakeupCount() : 0)
                .sleepQualityScore(unifiedAnalysis.getUnifiedSleepScore() != null ? unifiedAnalysis.getUnifiedSleepScore() : 0)
                .recordDate(startTime.toLocalDate())
                .wearableSource(unifiedAnalysis.getOriginalSource())
                .build();
    }

    private boolean shouldPerformAdditionalAiAnalysis(UnifiedSleepAnalysisDto unifiedAnalysis) {
        // 플랫폼에서 제공된 분석만으로 충분하지 않은 경우 추가 분석을 결정하는 로직
        // 예: 수면 점수가 특정 임계값 미만이거나, 사용자가 추가 분석을 요청한 경우
        return unifiedAnalysis.getUnifiedSleepScore() != null && unifiedAnalysis.getUnifiedSleepScore() < 70;
    }

    /**
     * Entity를 ResponseDto로 변환
     */
    private SleepRecordResponseDto convertToResponseDto(SleepRecord sleepRecord) {
        return SleepRecordResponseDto.builder()
                .id(sleepRecord.getId())
                .userId(sleepRecord.getUser().getId())
                .sleepStartTime(sleepRecord.getSleepStartTime())
                .sleepEndTime(sleepRecord.getSleepEndTime())
                .totalSleepMinutes(sleepRecord.getTotalSleepMinutes())
                .deepSleepMinutes(sleepRecord.getDeepSleepMinutes())
                .lightSleepMinutes(sleepRecord.getLightSleepMinutes())
                .remSleepMinutes(sleepRecord.getRemSleepMinutes())
                .wakeupCount(sleepRecord.getWakeupCount())
                .sleepInBedMinutes(sleepRecord.getSleepInBedMinutes())
                .sleepAwakeMinutes(sleepRecord.getSleepAwakeMinutes())
                .lightLevel(sleepRecord.getLightLevel())
                .noiseLevel(sleepRecord.getNoiseLevel())
                .temperature(sleepRecord.getTemperature())
                .humidity(sleepRecord.getHumidity())
                .heartRateData(sleepRecord.getHeartRateData())
                .respiratoryRateData(sleepRecord.getRespiratoryRateData())
                .snoreDetected(sleepRecord.getSnoreDetected())
                .bruxismDetected(sleepRecord.getBruxismDetected())
                .sleepTalkDetected(sleepRecord.getSleepTalkDetected())
                .audioDataPath(sleepRecord.getAudioDataPath())
                .wearableSource(sleepRecord.getWearableSource())
                .sleepQualityScore(sleepRecord.getSleepQualityScore())
                .userSatisfaction(sleepRecord.getUserSatisfaction())
                .recordDate(sleepRecord.getRecordDate())
                .createdAt(sleepRecord.getCreatedAt())
                .updatedAt(sleepRecord.getUpdatedAt())
                // 계산된 값들 추가
                .sleepEfficiency(sleepRecord.calculateSleepEfficiency())
                .deepSleepRatio(sleepRecord.calculateDeepSleepRatio())
                .lightSleepRatio(sleepRecord.calculateLightSleepRatio())
                .remSleepRatio(sleepRecord.calculateRemSleepRatio())
                .build();
    }

    private SleepRecord updateExistingRecord(SleepRecord existingRecord, UnifiedSleepAnalysisDto unifiedAnalysis) {
        SleepSessionInfo sessionInfo = unifiedAnalysis.getSleepSession();
        SleepStageAnalysis stageAnalysis = unifiedAnalysis.getSleepStages();
        
        return existingRecord.toBuilder()
                .sleepStartTime(sessionInfo.getSleepTime())
                .sleepEndTime(sessionInfo.getWakeTime())
                .totalSleepMinutes(sessionInfo.getTotalSleepTime())
                .deepSleepMinutes(stageAnalysis != null && stageAnalysis.getDeepSleepMinutes() != null ? stageAnalysis.getDeepSleepMinutes() : 0)
                .lightSleepMinutes(stageAnalysis != null && stageAnalysis.getLightSleepMinutes() != null ? stageAnalysis.getLightSleepMinutes() : 0)
                .remSleepMinutes(stageAnalysis != null && stageAnalysis.getRemSleepMinutes() != null ? stageAnalysis.getRemSleepMinutes() : 0)
                .wakeupCount(sessionInfo.getWakeupCount() != null ? sessionInfo.getWakeupCount() : 0)
                .sleepQualityScore(unifiedAnalysis.getUnifiedSleepScore() != null ? unifiedAnalysis.getUnifiedSleepScore() : 0)
                .build();
    }

    // ==================== 오디오 이벤트 메타데이터 관리 ====================

    /**
     * 수면 오디오 이벤트 메타데이터 생성
     */
    @Transactional
    public SleepAudioEventResponseDto createAudioEventMetadata(Long userId, Long recordId, SleepAudioEventRequestDto requestDto) {
        log.info("Creating audio event metadata for sleep record: {} by user: {}", recordId, userId);
        
        SleepRecord sleepRecord = findSleepRecordByIdAndUserId(recordId, userId);
        
        // Request DTO를 Entity로 변환
        SleepAudioEvent audioEvent = buildAudioEventFromRequest(sleepRecord, requestDto);
        
        // 수면 기록에 오디오 이벤트 추가
        sleepRecord.getAudioEvents().add(audioEvent);
        
        // 변경사항 저장
        sleepRecordRepository.save(sleepRecord);
        
        log.info("Audio event metadata created successfully with ID: {}", audioEvent.getId());
        
        return convertToAudioEventResponseDto(audioEvent);
    }

    /**
     * 수면 기록의 오디오 이벤트 목록 조회
     */
    public List<SleepAudioEventResponseDto> getAudioEvents(Long userId, Long recordId, AudioEventType eventType) {
        log.info("Getting audio events for sleep record: {} by user: {}, filter: {}", recordId, userId, eventType);
        
        SleepRecord sleepRecord = findSleepRecordByIdAndUserId(recordId, userId);
        
        return sleepRecord.getAudioEvents().stream()
                .filter(event -> eventType == null || event.getEventType() == eventType)
                .map(this::convertToAudioEventResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 오디오 이벤트 조회
     */
    public SleepAudioEventResponseDto getAudioEvent(Long userId, Long recordId, Long eventId) {
        log.info("Getting audio event: {} for sleep record: {} by user: {}", eventId, recordId, userId);
        
        SleepRecord sleepRecord = findSleepRecordByIdAndUserId(recordId, userId);
        
        SleepAudioEvent audioEvent = sleepRecord.getAudioEvents().stream()
                .filter(event -> event.getId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("오디오 이벤트를 찾을 수 없습니다: " + eventId));
        
        return convertToAudioEventResponseDto(audioEvent);
    }

    /**
     * 배치 오디오 이벤트 메타데이터 생성
     */
    @Transactional
    public List<SleepAudioEventResponseDto> createAudioEventMetadataBatch(Long userId, Long recordId, List<SleepAudioEventRequestDto> requestDtos) {
        log.info("Creating {} audio event metadata in batch for sleep record: {} by user: {}", 
                requestDtos.size(), recordId, userId);
        
        SleepRecord sleepRecord = findSleepRecordByIdAndUserId(recordId, userId);
        
        List<SleepAudioEvent> audioEvents = requestDtos.stream()
                .map(requestDto -> buildAudioEventFromRequest(sleepRecord, requestDto))
                .collect(Collectors.toList());
        
        // 수면 기록에 모든 오디오 이벤트 추가
        sleepRecord.getAudioEvents().addAll(audioEvents);
        
        // 변경사항 저장
        sleepRecordRepository.save(sleepRecord);
        
        log.info("Batch audio event metadata created successfully: {} events", audioEvents.size());
        
        return audioEvents.stream()
                .map(this::convertToAudioEventResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Request DTO를 Entity로 변환
     */
    private SleepAudioEvent buildAudioEventFromRequest(SleepRecord sleepRecord, SleepAudioEventRequestDto requestDto) {
        return SleepAudioEvent.builder()
                .user(sleepRecord.getUser())
                .sleepRecord(sleepRecord)
                .eventType(requestDto.getEventType())
                .eventDate(requestDto.getEventStartTime().toLocalDate().atStartOfDay())
                .eventStartTime(requestDto.getEventStartTime())
                .eventEndTime(requestDto.getEventEndTime())
                .durationSeconds(requestDto.getDurationSeconds())
                .intensityLevel(requestDto.getIntensityLevel())
                .confidenceScore(requestDto.getConfidenceScore())
                .frequencyPerHour(requestDto.getFrequencyPerHour())
                .decibelLevel(requestDto.getDecibelLevel())
                .dataSource(requestDto.getDataSource())
                .platformEventId(requestDto.getPlatformEventId())
                .additionalMetadata(requestDto.getAdditionalMetadata())
                .build();
    }

    /**
     * Entity를 Response DTO로 변환
     */
    private SleepAudioEventResponseDto convertToAudioEventResponseDto(SleepAudioEvent audioEvent) {
        return SleepAudioEventResponseDto.builder()
                .id(audioEvent.getId())
                .sleepRecordId(audioEvent.getSleepRecord().getId())
                .eventType(audioEvent.getEventType())
                .eventTypeDisplayName(audioEvent.getEventType().getDisplayName())
                .eventDate(audioEvent.getEventDate())
                .eventStartTime(audioEvent.getEventStartTime())
                .eventEndTime(audioEvent.getEventEndTime())
                .durationSeconds(audioEvent.getDurationSeconds())
                .intensityLevel(audioEvent.getIntensityLevel())
                .confidenceScore(audioEvent.getConfidenceScore())
                .frequencyPerHour(audioEvent.getFrequencyPerHour())
                .decibelLevel(audioEvent.getDecibelLevel())
                .dataSource(audioEvent.getDataSource())
                .platformEventId(audioEvent.getPlatformEventId())
                .additionalMetadata(audioEvent.getAdditionalMetadata())
                .createdAt(audioEvent.getCreatedAt())
                .updatedAt(audioEvent.getUpdatedAt())
                // 분석 결과 추가
                .isHighIntensity(audioEvent.isHighIntensity())
                .requiresMedicalAttention(audioEvent.requiresMedicalAttention())
                .severityLevel(audioEvent.calculateSeverityLevel())
                .averageIntensityPerMinute(audioEvent.calculateAverageIntensityPerMinute())
                .sleepQualityImpact(audioEvent.isHighIntensity() ? 75.0 : 25.0)
                .build();
    }
} 