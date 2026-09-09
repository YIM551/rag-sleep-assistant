package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.SleepStageRequestDto;
import com.sleepwell.sleepwell_backend.dto.SleepStageResponseDto;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.SleepStage;
import com.sleepwell.sleepwell_backend.enums.SleepStageType;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import com.sleepwell.sleepwell_backend.repository.SleepStageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 수면 단계 비즈니스 로직 서비스
 *
 * 수면 단계와 관련된 모든 비즈니스 로직을 처리합니다.
 * 김영한 스타일의 서비스 레이어 구현으로 도메인 로직과
 * 인프라스트럭처 관심사를 분리합니다.
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SleepStageService {

    private final SleepStageRepository sleepStageRepository;
    private final SleepRecordRepository sleepRecordRepository;

    // === 수면 단계 CRUD 메서드 ===

    /**
     * 수면 단계 생성
     *
     * @param sleepRecordId 수면 기록 ID
     * @param requestDto 수면 단계 생성 요청 데이터
     * @return 생성된 수면 단계 응답 DTO
     * @throws BusinessException 수면 기록을 찾을 수 없거나 데이터 검증 실패 시
     */
    @Transactional
    public SleepStageResponseDto createSleepStage(Long sleepRecordId, SleepStageRequestDto requestDto) {
        log.info("Creating sleep stage for record: {}, type: {}", sleepRecordId, requestDto.getStageType());

        // 요청 데이터 검증
        requestDto.validateTimeConsistency();
        requestDto.validateStageTypeConsistency();

        // 수면 기록 존재 확인
        SleepRecord sleepRecord = sleepRecordRepository.findById(sleepRecordId)
                .orElseThrow(() -> new BusinessException(
                        "수면 기록을 찾을 수 없습니다: " + sleepRecordId,
                        HttpStatus.NOT_FOUND,
                        "SLEEP_RECORD_NOT_FOUND"));

        // 수면 단계가 수면 기록의 시간 범위 내에 있는지 검증
        validateStageWithinSleepRecord(sleepRecord, requestDto.getStartTime(), requestDto.getEndTime());

        // 기존 수면 단계와 겹치는지 검증 (선택적)
        validateNoOverlappingStages(sleepRecordId, requestDto.getStartTime(), requestDto.getEndTime());

        // 엔티티 생성
        SleepStage sleepStage = SleepStage.builder()
                .sleepRecord(sleepRecord)
                .stageType(requestDto.getStageType())
                .startTime(requestDto.getStartTime())
                .endTime(requestDto.getEndTime())
                .confidenceScore(requestDto.getConfidenceScore())
                .metadata(requestDto.getMetadata())
                .build();

        // 지속 시간 자동 계산
        sleepStage.updateDurationMinutes();

        SleepStage savedStage = sleepStageRepository.save(sleepStage);
        log.info("Sleep stage created successfully: {} (duration: {} minutes)",
                savedStage.getId(), savedStage.getDurationMinutes());

        return SleepStageResponseDto.from(savedStage);
    }

    /**
     * 특정 수면 기록의 모든 수면 단계 조회
     *
     * @param sleepRecordId 수면 기록 ID
     * @return 수면 단계 목록 (시간 순서대로)
     */
    public List<SleepStageResponseDto> getSleepStagesBySleepRecord(Long sleepRecordId) {
        log.debug("Fetching sleep stages for record: {}", sleepRecordId);

        List<SleepStage> stages = sleepStageRepository.findBySleepRecordIdOrderByStartTimeAsc(sleepRecordId);

        return stages.stream()
                .map(SleepStageResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 수면 기록의 특정 타입 수면 단계 조회
     *
     * @param sleepRecordId 수면 기록 ID
     * @param stageType 수면 단계 타입
     * @return 해당 타입의 수면 단계 목록
     */
    public List<SleepStageResponseDto> getSleepStagesByType(Long sleepRecordId, SleepStageType stageType) {
        log.debug("Fetching {} stages for record: {}", stageType, sleepRecordId);

        List<SleepStage> stages = sleepStageRepository
                .findBySleepRecordIdAndStageTypeOrderByStartTimeAsc(sleepRecordId, stageType);

        return stages.stream()
                .map(SleepStageResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 수면 단계 수정
     *
     * @param stageId 수면 단계 ID
     * @param requestDto 수정 요청 데이터
     * @return 수정된 수면 단계 응답 DTO
     */
    @Transactional
    public SleepStageResponseDto updateSleepStage(Long stageId, SleepStageRequestDto requestDto) {
        log.info("Updating sleep stage: {}", stageId);

        // 요청 데이터 검증
        requestDto.validateTimeConsistency();
        requestDto.validateStageTypeConsistency();

        SleepStage sleepStage = sleepStageRepository.findById(stageId)
                .orElseThrow(() -> new BusinessException(
                        "수면 단계를 찾을 수 없습니다: " + stageId,
                        HttpStatus.NOT_FOUND,
                        "SLEEP_STAGE_NOT_FOUND"));

        SleepRecord sleepRecord = sleepStage.getSleepRecord();

        // 수면 기록 범위 검증
        validateStageWithinSleepRecord(sleepRecord, requestDto.getStartTime(), requestDto.getEndTime());

        // 다른 수면 단계와의 겹침 검증 (자기 자신 제외)
        validateNoOverlappingStagesExcept(sleepRecord.getId(), requestDto.getStartTime(), requestDto.getEndTime(), stageId);

        // 엔티티 도메인 메서드를 통한 수정
        sleepStage.changeStageType(requestDto.getStageType());
        sleepStage.changeTimeRange(requestDto.getStartTime(), requestDto.getEndTime());

        if (requestDto.getConfidenceScore() != null) {
            sleepStage.changeConfidenceScore(requestDto.getConfidenceScore());
        }

        SleepStage savedStage = sleepStageRepository.save(sleepStage);
        log.info("Sleep stage updated successfully: {}", savedStage.getId());

        return SleepStageResponseDto.from(savedStage);
    }

    /**
     * 수면 단계 삭제
     *
     * @param stageId 수면 단계 ID
     */
    @Transactional
    public void deleteSleepStage(Long stageId) {
        log.info("Deleting sleep stage: {}", stageId);

        if (!sleepStageRepository.existsById(stageId)) {
            throw new BusinessException(
                    "수면 단계를 찾을 수 없습니다: " + stageId,
                    HttpStatus.NOT_FOUND,
                    "SLEEP_STAGE_NOT_FOUND");
        }

        sleepStageRepository.deleteById(stageId);
        log.info("Sleep stage deleted successfully: {}", stageId);
    }

    // === 통계 및 분석 메서드 ===

    /**
     * 특정 수면 기록의 수면 단계별 통계
     *
     * @param sleepRecordId 수면 기록 ID
     * @return 단계별 총 시간 (분) 맵
     */
    public Map<SleepStageType, Integer> getSleepStageStatistics(Long sleepRecordId) {
        log.debug("Calculating sleep stage statistics for record: {}", sleepRecordId);

        List<Object[]> statistics = sleepStageRepository.findStageStatistics(sleepRecordId);

        return statistics.stream()
                .collect(Collectors.toMap(
                        row -> (SleepStageType) row[0],
                        row -> ((Number) row[1]).intValue()
                ));
    }

    /**
     * 회복성 수면 단계만 조회 (DEEP + REM)
     *
     * @param sleepRecordId 수면 기록 ID
     * @return 회복성 수면 단계 목록
     */
    public List<SleepStageResponseDto> getRestorativeSleepStages(Long sleepRecordId) {
        log.debug("Fetching restorative sleep stages for record: {}", sleepRecordId);

        List<SleepStage> stages = sleepStageRepository.findRestorativeSleepStages(sleepRecordId);

        return stages.stream()
                .map(SleepStageResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 각성 단계만 조회
     *
     * @param sleepRecordId 수면 기록 ID
     * @return 각성 단계 목록
     */
    public List<SleepStageResponseDto> getAwakeStages(Long sleepRecordId) {
        log.debug("Fetching awake stages for record: {}", sleepRecordId);

        List<SleepStage> stages = sleepStageRepository.findAwakeStages(sleepRecordId);

        return stages.stream()
                .map(SleepStageResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 수면 주기 개수 계산 (REM 횟수 기준)
     *
     * @param sleepRecordId 수면 기록 ID
     * @return 수면 주기 개수
     */
    public long countSleepCycles(Long sleepRecordId) {
        log.debug("Counting sleep cycles for record: {}", sleepRecordId);
        return sleepStageRepository.countSleepCycles(sleepRecordId);
    }

    /**
     * 첫 REM 수면까지의 시간 계산 (REM Latency)
     *
     * @param sleepRecordId 수면 기록 ID
     * @return REM latency (분), REM이 없으면 null
     */
    public Integer calculateRemLatency(Long sleepRecordId) {
        log.debug("Calculating REM latency for record: {}", sleepRecordId);

        SleepRecord sleepRecord = sleepRecordRepository.findById(sleepRecordId)
                .orElseThrow(() -> new BusinessException(
                        "수면 기록을 찾을 수 없습니다: " + sleepRecordId,
                        HttpStatus.NOT_FOUND,
                        "SLEEP_RECORD_NOT_FOUND"));

        LocalDateTime firstRemStart = sleepStageRepository.findFirstRemStartTime(sleepRecordId);

        if (firstRemStart == null) {
            return null;
        }

        return (int) java.time.temporal.ChronoUnit.MINUTES.between(
                sleepRecord.getSleepStartTime(), firstRemStart);
    }

    // === Private 검증 메서드 ===

    /**
     * 수면 단계가 수면 기록의 시간 범위 내에 있는지 검증
     */
    private void validateStageWithinSleepRecord(SleepRecord sleepRecord,
                                                  LocalDateTime startTime,
                                                  LocalDateTime endTime) {
        if (startTime.isBefore(sleepRecord.getSleepStartTime()) ||
            endTime.isAfter(sleepRecord.getSleepEndTime())) {
            throw new BusinessException(
                    "수면 단계 시간이 수면 기록 범위를 벗어납니다",
                    HttpStatus.BAD_REQUEST,
                    "STAGE_OUT_OF_SLEEP_RECORD_RANGE");
        }
    }

    /**
     * 기존 수면 단계와 시간이 겹치는지 검증
     */
    private void validateNoOverlappingStages(Long sleepRecordId,
                                              LocalDateTime startTime,
                                              LocalDateTime endTime) {
        List<SleepStage> existingStages = sleepStageRepository
                .findBySleepRecordIdOrderByStartTimeAsc(sleepRecordId);

        for (SleepStage existing : existingStages) {
            if (startTime.isBefore(existing.getEndTime()) && endTime.isAfter(existing.getStartTime())) {
                log.warn("New stage overlaps with existing stage: {}", existing.getId());
                throw new BusinessException(
                        "수면 단계가 기존 단계와 겹칩니다",
                        HttpStatus.CONFLICT,
                        "STAGE_TIME_OVERLAP");
            }
        }
    }

    /**
     * 기존 수면 단계와 시간이 겹치는지 검증 (특정 ID 제외)
     * 수정 시 자기 자신을 제외하고 검증
     */
    private void validateNoOverlappingStagesExcept(Long sleepRecordId,
                                                     LocalDateTime startTime,
                                                     LocalDateTime endTime,
                                                     Long exceptStageId) {
        List<SleepStage> existingStages = sleepStageRepository
                .findBySleepRecordIdOrderByStartTimeAsc(sleepRecordId);

        for (SleepStage existing : existingStages) {
            // 자기 자신은 검증에서 제외
            if (existing.getId().equals(exceptStageId)) {
                continue;
            }

            if (startTime.isBefore(existing.getEndTime()) && endTime.isAfter(existing.getStartTime())) {
                log.warn("Updated stage overlaps with existing stage: {}", existing.getId());
                throw new BusinessException(
                        "수면 단계가 기존 단계와 겹칩니다",
                        HttpStatus.CONFLICT,
                        "STAGE_TIME_OVERLAP");
            }
        }
    }
}
