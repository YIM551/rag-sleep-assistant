package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.entity.SleepDiary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 수면일지 응답 DTO
 *
 * 수면일지 조회 시 클라이언트에게 전달되는 데이터 구조입니다.
 * 엔티티의 모든 정보와 계산된 지표들을 포함합니다.
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "수면일지 응답")
public class SleepDiaryResponseDto {

    @Schema(description = "수면일지 ID", example = "1")
    private Long id;

    @Schema(description = "사용자 ID", example = "123")
    private Long userId;

    @Schema(description = "일지 날짜", example = "2024-01-15")
    private LocalDate diaryDate;

    // === 수치화 기록 ===

    @Schema(description = "낮잠 시간 (분)", example = "30")
    private Integer napMinutes;

    @Schema(description = "침대에 누운 시간", example = "23:30:00")
    private LocalTime bedTime;

    @Schema(description = "주관적 수면 시간 (분)", example = "420")
    private Integer perceivedSleepMinutes;

    @Schema(description = "각성 횟수", example = "2")
    private Integer awakeningCount;

    @Schema(description = "총 각성 시간 (분)", example = "20")
    private Integer totalAwakeMinutes;

    @Schema(description = "기상 시간", example = "07:00:00")
    private LocalTime wakeUpTime;

    @Schema(description = "카페인 섭취량 (mg)", example = "150")
    private Integer caffeineMg;

    @Schema(description = "알코올 섭취량 (ml)", example = "100")
    private Integer alcoholMl;

    @Schema(description = "수면제 및 약물 복용량 (mg)", example = "5.0")
    private Double medicationMg;

    // === 단순 기록 ===

    @Schema(description = "운동 여부", example = "true")
    private Boolean didExercise;

    @Schema(description = "자유 기재 일기", example = "오늘은 스트레스가 많아서 잠들기 어려웠다")
    private String diaryNotes;

    // === 주관적 평가 ===

    @Schema(description = "주관적 수면 품질 점수 (1-10)", example = "7")
    private Integer subjectiveSleepQuality;

    @Schema(description = "아침 기상 시 컨디션 점수 (1-10)", example = "6")
    private Integer morningConditionScore;

    @Schema(description = "스트레스 레벨 (1-10)", example = "8")
    private Integer stressLevel;

    @Schema(description = "주관적 졸림 정도 (1-10, 1: 각성, 10: 졸림)", example = "5")
    private Integer sleepiness;

    @Schema(description = "멜라토닌 섭취량 (mg)", example = "3.0")
    private Double melatoninIntake;

    // === 계산된 지표들 ===

    @Schema(description = "총 침상 시간 (분)", example = "450")
    private Integer totalBedTime;

    @Schema(description = "주관적 수면 효율성 (%)", example = "93.3")
    private Double subjectiveSleepEfficiency;

    @Schema(description = "카페인 섭취 레벨", example = "MODERATE")
    private String caffeineIntakeLevel;

    @Schema(description = "알코올 섭취 레벨", example = "LIGHT")
    private String alcoholIntakeLevel;

    @Schema(description = "종합 생활습관 점수 (0-100)", example = "75.5")
    private Double lifestyleScore;

    // === 메타데이터 ===

    @Schema(description = "생성일시", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-15T14:20:00")
    private LocalDateTime updatedAt;

    /**
     * SleepDiary 엔티티로부터 ResponseDto를 생성하는 정적 메서드
     * 김영한 스타일: 엔티티 → DTO 변환 로직
     *
     * @param sleepDiary 수면일지 엔티티
     * @return 변환된 응답 DTO
     */
    public static SleepDiaryResponseDto from(SleepDiary sleepDiary) {
        return SleepDiaryResponseDto.builder()
                .id(sleepDiary.getId())
                .userId(sleepDiary.getUser().getId())
                .diaryDate(sleepDiary.getDiaryDate())

                // 수치화 기록
                .napMinutes(sleepDiary.getNapMinutes())
                .bedTime(sleepDiary.getBedTime())
                .perceivedSleepMinutes(sleepDiary.getPerceivedSleepMinutes())
                .awakeningCount(sleepDiary.getAwakeningCount())
                .totalAwakeMinutes(sleepDiary.getTotalAwakeMinutes())
                .wakeUpTime(sleepDiary.getWakeUpTime())
                .caffeineMg(sleepDiary.getCaffeineMg())
                .alcoholMl(sleepDiary.getAlcoholMl())
                .medicationMg(sleepDiary.getMedicationMg())

                // 단순 기록
                .didExercise(sleepDiary.getDidExercise())
                .diaryNotes(sleepDiary.getDiaryNotes())

                // 주관적 평가
                .subjectiveSleepQuality(sleepDiary.getSubjectiveSleepQuality())
                .morningConditionScore(sleepDiary.getMorningConditionScore())
                .stressLevel(sleepDiary.getStressLevel())
                .sleepiness(sleepDiary.getSleepiness())
                .melatoninIntake(sleepDiary.getMelatoninIntake())

                // 계산된 지표들 (Optional 처리)
                .totalBedTime(sleepDiary.calculateTotalBedTime().orElse(null))
                .subjectiveSleepEfficiency(sleepDiary.calculateSubjectiveSleepEfficiency().orElse(null))
                .caffeineIntakeLevel(sleepDiary.getCaffeineIntakeLevel().name())
                .alcoholIntakeLevel(sleepDiary.getAlcoholIntakeLevel().name())
                .lifestyleScore(sleepDiary.calculateLifestyleScore())

                // 메타데이터
                .createdAt(sleepDiary.getCreatedAt())
                .updatedAt(sleepDiary.getUpdatedAt())
                .build();
    }

    /**
     * 요약된 버전의 DTO 생성 (리스트 조회용)
     * 계산 비용이 높은 필드들을 제외한 기본 정보만 포함
     *
     * @param sleepDiary 수면일지 엔티티
     * @return 요약된 응답 DTO
     */
    public static SleepDiaryResponseDto fromSummary(SleepDiary sleepDiary) {
        return SleepDiaryResponseDto.builder()
                .id(sleepDiary.getId())
                .userId(sleepDiary.getUser().getId())
                .diaryDate(sleepDiary.getDiaryDate())
                .subjectiveSleepQuality(sleepDiary.getSubjectiveSleepQuality())
                .morningConditionScore(sleepDiary.getMorningConditionScore())
                .stressLevel(sleepDiary.getStressLevel())
                .didExercise(sleepDiary.getDidExercise())
                .createdAt(sleepDiary.getCreatedAt())
                .updatedAt(sleepDiary.getUpdatedAt())
                .build();
    }
}