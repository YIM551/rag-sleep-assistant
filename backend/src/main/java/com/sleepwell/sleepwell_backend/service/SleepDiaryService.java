package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.SleepDiaryRequestDto;
import com.sleepwell.sleepwell_backend.dto.SleepDiaryResponseDto;
import com.sleepwell.sleepwell_backend.entity.SleepDiary;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.repository.SleepDiaryRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 수면일지 비즈니스 로직 서비스
 *
 * 수면일지와 관련된 모든 비즈니스 로직을 처리합니다.
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
public class SleepDiaryService {

    private final SleepDiaryRepository sleepDiaryRepository;
    private final UserRepository userRepository;

    // === 수면일지 CRUD 메서드 ===

    /**
     * 수면일지 생성
     * 하루에 하나의 수면일지만 작성 가능한 비즈니스 규칙 적용
     *
     * @param userId 사용자 ID
     * @param requestDto 수면일지 생성 요청 데이터
     * @return 생성된 수면일지 응답 DTO
     * @throws BusinessException 이미 해당 날짜의 일지가 존재하거나 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public SleepDiaryResponseDto createSleepDiary(Long userId, SleepDiaryRequestDto requestDto) {
        log.info("Creating sleep diary for user: {}, date: {}", userId, requestDto.getDiaryDate());

        // 요청 데이터 검증
        requestDto.validateSleepTimeConsistency();
        requestDto.validateLifestyleConsistency();

        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        "사용자를 찾을 수 없습니다: " + userId,
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND"));

        // 중복 날짜 검증
        sleepDiaryRepository.findByUserIdAndDiaryDate(userId, requestDto.getDiaryDate())
                .ifPresent(existingDiary -> {
                    throw new BusinessException(
                            "해당 날짜의 수면일지가 이미 존재합니다: " + requestDto.getDiaryDate(),
                            HttpStatus.CONFLICT,
                            "DIARY_ALREADY_EXISTS");
                });

        // 엔티티 생성 및 도메인 로직 적용
        SleepDiary sleepDiary = SleepDiary.builder()
                .user(user)
                .diaryDate(requestDto.getDiaryDate())
                .napMinutes(requestDto.getNapMinutes())
                .bedTime(requestDto.getBedTime())
                .perceivedSleepMinutes(requestDto.getPerceivedSleepMinutes())
                .awakeningCount(requestDto.getAwakeningCount())
                .totalAwakeMinutes(requestDto.getTotalAwakeMinutes())
                .wakeUpTime(requestDto.getWakeUpTime())
                .caffeineMg(requestDto.getCaffeineMg())
                .alcoholMl(requestDto.getAlcoholMl())
                .medicationMg(requestDto.getMedicationMg())
                .didExercise(requestDto.getDidExercise())
                .diaryNotes(requestDto.getDiaryNotes())
                .subjectiveSleepQuality(requestDto.getSubjectiveSleepQuality())
                .morningConditionScore(requestDto.getMorningConditionScore())
                .stressLevel(requestDto.getStressLevel())
                .sleepiness(requestDto.getSleepiness())
                .melatoninIntake(requestDto.getMelatoninIntake())
                .build();

        // 도메인 규칙 검증 (엔티티 메서드 활용)
        if (requestDto.getCaffeineMg() != null) {
            sleepDiary.changeCaffeineIntake(requestDto.getCaffeineMg());
        }
        if (requestDto.getAlcoholMl() != null) {
            sleepDiary.changeAlcoholIntake(requestDto.getAlcoholMl());
        }

        SleepDiary savedDiary = sleepDiaryRepository.save(sleepDiary);
        log.info("Sleep diary created successfully: {}", savedDiary.getId());

        return SleepDiaryResponseDto.from(savedDiary);
    }

    /**
     * 특정 수면일지 조회
     * 사용자 권한 검증 포함
     *
     * @param userId 사용자 ID
     * @param diaryId 수면일지 ID
     * @return 수면일지 응답 DTO
     * @throws BusinessException 일지를 찾을 수 없거나 접근 권한이 없는 경우
     */
    public SleepDiaryResponseDto getSleepDiary(Long userId, Long diaryId) {
        log.info("Getting sleep diary: {} for user: {}", diaryId, userId);

        SleepDiary sleepDiary = sleepDiaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException(
                        "수면일지를 찾을 수 없습니다: " + diaryId,
                        HttpStatus.NOT_FOUND,
                        "DIARY_NOT_FOUND"));

        // 사용자 권한 검증
        validateUserAccess(userId, sleepDiary);

        return SleepDiaryResponseDto.from(sleepDiary);
    }

    /**
     * 특정 날짜의 수면일지 조회
     *
     * @param userId 사용자 ID
     * @param date 조회할 날짜
     * @return 수면일지 응답 DTO
     * @throws BusinessException 해당 날짜의 일지가 없는 경우
     */
    public SleepDiaryResponseDto getSleepDiaryByDate(Long userId, LocalDate date) {
        log.info("Getting sleep diary by date: {} for user: {}", date, userId);

        SleepDiary sleepDiary = sleepDiaryRepository.findByUserIdAndDiaryDate(userId, date)
                .orElseThrow(() -> new BusinessException(
                        "해당 날짜의 수면일지를 찾을 수 없습니다: " + date,
                        HttpStatus.NOT_FOUND,
                        "DIARY_NOT_FOUND_FOR_DATE"));

        return SleepDiaryResponseDto.from(sleepDiary);
    }

    /**
     * 사용자의 수면일지 목록 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param sortBy 정렬 기준 필드
     * @param sortDir 정렬 방향 (asc/desc)
     * @return 수면일지 페이지
     */
    public Page<SleepDiaryResponseDto> getSleepDiaries(
            Long userId, int page, int size, String sortBy, String sortDir) {
        log.info("Getting sleep diaries for user: {}, page: {}, size: {}", userId, page, size);

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return sleepDiaryRepository.findByUserIdOrderByDiaryDateDesc(userId, pageable)
                .map(SleepDiaryResponseDto::fromSummary); // 리스트 조회 시에는 요약 버전 사용
    }

    /**
     * 기간별 수면일지 조회
     *
     * @param userId 사용자 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param pageable 페이징 정보
     * @return 기간 내 수면일지 페이지
     * @throws BusinessException 잘못된 날짜 범위인 경우
     */
    public Page<SleepDiaryResponseDto> getSleepDiariesBetweenDates(
            Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Getting sleep diaries for user: {} between {} and {}", userId, startDate, endDate);

        validateDateRange(startDate, endDate);

        return sleepDiaryRepository.findByUserIdAndDiaryDateBetween(userId, startDate, endDate, pageable)
                .map(SleepDiaryResponseDto::from);
    }

    /**
     * 수면일지 수정
     *
     * @param userId 사용자 ID
     * @param diaryId 수면일지 ID
     * @param requestDto 수정할 데이터
     * @return 수정된 수면일지 응답 DTO
     * @throws BusinessException 일지를 찾을 수 없거나 접근 권한이 없는 경우
     */
    @Transactional
    public SleepDiaryResponseDto updateSleepDiary(
            Long userId, Long diaryId, SleepDiaryRequestDto requestDto) {
        log.info("Updating sleep diary: {} for user: {}", diaryId, userId);

        // 부분 업데이트이므로 검증은 도메인 메서드 내에서 필요시 수행
        // 전체 필드 검증은 생성 시에만 수행

        SleepDiary sleepDiary = sleepDiaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException(
                        "수면일지를 찾을 수 없습니다: " + diaryId,
                        HttpStatus.NOT_FOUND,
                        "DIARY_NOT_FOUND"));

        // 사용자 권한 검증
        validateUserAccess(userId, sleepDiary);

        // 도메인 메서드를 사용한 null-safe 업데이트
        // null이 아닌 필드만 업데이트하고, null인 필드는 기존 값 유지
        sleepDiary.updateDiary(requestDto);

        // 변경 감지(Dirty Checking)에 의해 자동으로 DB에 반영되지만, 명시적으로 save 호출
        SleepDiary savedDiary = sleepDiaryRepository.save(sleepDiary);
        log.info("Sleep diary updated successfully: {}", savedDiary.getId());

        return SleepDiaryResponseDto.from(savedDiary);
    }

    /**
     * 수면일지 삭제
     *
     * @param userId 사용자 ID
     * @param diaryId 수면일지 ID
     * @throws BusinessException 일지를 찾을 수 없거나 접근 권한이 없는 경우
     */
    @Transactional
    public void deleteSleepDiary(Long userId, Long diaryId) {
        log.info("Deleting sleep diary: {} for user: {}", diaryId, userId);

        SleepDiary sleepDiary = sleepDiaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException(
                        "수면일지를 찾을 수 없습니다: " + diaryId,
                        HttpStatus.NOT_FOUND,
                        "DIARY_NOT_FOUND"));

        // 사용자 권한 검증
        validateUserAccess(userId, sleepDiary);

        sleepDiaryRepository.delete(sleepDiary);
        log.info("Sleep diary deleted successfully: {}", diaryId);
    }

    // === 통계 및 분석 메서드 ===

    /**
     * 최근 30일간 수면일지 조회
     *
     * @param userId 사용자 ID
     * @return 최근 30일간의 수면일지 목록
     */
    public List<SleepDiaryResponseDto> getRecentSleepDiaries(Long userId) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        return sleepDiaryRepository
                .findTop30ByUserIdAndDiaryDateGreaterThanEqualOrderByDiaryDateDesc(userId, thirtyDaysAgo)
                .stream()
                .map(SleepDiaryResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 수면 품질 통계 조회
     *
     * @param userId 사용자 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 품질 점수별 통계
     */
    public Map<Integer, Long> getSleepQualityStatistics(
            Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Getting sleep quality statistics for user: {} between {} and {}",
                userId, startDate, endDate);

        validateDateRange(startDate, endDate);

        List<Object[]> results = sleepDiaryRepository
                .findSleepQualityStatistics(userId, startDate, endDate);

        return results.stream()
                .collect(Collectors.toMap(
                        result -> (Integer) result[0], // score
                        result -> (Long) result[1]     // count
                ));
    }

    /**
     * 생활습관이 수면에 미치는 영향 분석
     *
     * @param userId 사용자 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 생활습관별 영향 분석 결과
     */
    public Map<String, Object> getLifestyleImpactAnalysis(
            Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Getting lifestyle impact analysis for user: {} between {} and {}",
                userId, startDate, endDate);

        validateDateRange(startDate, endDate);

        List<Object[]> results = sleepDiaryRepository
                .findLifestyleImpactStatistics(userId, startDate, endDate);

        // 결과를 Map으로 변환하여 반환
        return results.stream()
                .collect(Collectors.groupingBy(
                        result -> ((Boolean) result[0]) ? "exercised" : "not_exercised",
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    Object[] data = list.get(0);
                                    return Map.of(
                                            "highCaffeineRate", (Double) data[1],
                                            "alcoholRate", (Double) data[2],
                                            "avgSleepQuality", (Double) data[3],
                                            "count", (Long) data[4]
                                    );
                                }
                        )
                ));
    }

    // === 헬퍼 메서드 ===

    /**
     * 사용자 접근 권한 검증
     */
    private void validateUserAccess(Long userId, SleepDiary sleepDiary) {
        if (!sleepDiary.getUser().getId().equals(userId)) {
            throw new BusinessException(
                    "해당 수면일지에 접근할 권한이 없습니다",
                    HttpStatus.FORBIDDEN,
                    "ACCESS_DENIED");
        }
    }

    /**
     * 날짜 범위 검증
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(
                    "시작 날짜가 종료 날짜보다 늦을 수 없습니다",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE");
        }

        // 최대 1년 범위 제한
        if (startDate.plusYears(1).isBefore(endDate)) {
            throw new BusinessException(
                    "조회 가능한 최대 기간은 1년입니다",
                    HttpStatus.BAD_REQUEST,
                    "DATE_RANGE_TOO_LARGE");
        }
    }
}