package com.sleepwell.sleepwell_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 수면일지 생성/수정 요청 DTO
 *
 * 사용자가 수면일지를 작성할 때 전달하는 데이터를 검증하고 처리합니다.
 * 모든 필드가 선택사항이며, 사용자가 원하는 항목만 기록할 수 있습니다.
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "수면일지 생성/수정 요청")
public class SleepDiaryRequestDto {

    @Schema(description = "일지 날짜", example = "2024-01-15", required = true)
    @NotNull(message = "일지 날짜는 필수입니다")
    @PastOrPresent(message = "일지 날짜는 오늘 이전이어야 합니다")
    private LocalDate diaryDate;

    // === 수치화 기록 ===

    @Schema(description = "낮잠 시간 (분)", example = "30", minimum = "0", maximum = "480")
    @Min(value = 0, message = "낮잠 시간은 0분 이상이어야 합니다")
    @Max(value = 480, message = "낮잠 시간은 8시간(480분)을 초과할 수 없습니다")
    private Integer napMinutes;

    @Schema(description = "침대에 누운 시간", example = "23:30:00")
    private LocalTime bedTime;

    @Schema(description = "주관적 수면 시간 (분)", example = "420", minimum = "0", maximum = "1440")
    @Min(value = 0, message = "주관적 수면 시간은 0분 이상이어야 합니다")
    @Max(value = 1440, message = "주관적 수면 시간은 24시간(1440분)을 초과할 수 없습니다")
    private Integer perceivedSleepMinutes;

    @Schema(description = "각성 횟수", example = "2", minimum = "0", maximum = "50")
    @Min(value = 0, message = "각성 횟수는 0회 이상이어야 합니다")
    @Max(value = 50, message = "각성 횟수는 50회를 초과할 수 없습니다")
    private Integer awakeningCount;

    @Schema(description = "총 각성 시간 (분)", example = "20", minimum = "0", maximum = "720")
    @Min(value = 0, message = "총 각성 시간은 0분 이상이어야 합니다")
    @Max(value = 720, message = "총 각성 시간은 12시간(720분)을 초과할 수 없습니다")
    private Integer totalAwakeMinutes;

    @Schema(description = "기상 시간", example = "07:00:00")
    private LocalTime wakeUpTime;

    @Schema(description = "카페인 섭취량 (mg)", example = "150", minimum = "0", maximum = "2000")
    @Min(value = 0, message = "카페인 섭취량은 0mg 이상이어야 합니다")
    @Max(value = 2000, message = "카페인 섭취량은 2000mg을 초과할 수 없습니다")
    private Integer caffeineMg;

    @Schema(description = "알코올 섭취량 (ml)", example = "100", minimum = "0", maximum = "1000")
    @Min(value = 0, message = "알코올 섭취량은 0ml 이상이어야 합니다")
    @Max(value = 1000, message = "알코올 섭취량은 1000ml를 초과할 수 없습니다")
    private Integer alcoholMl;

    @Schema(description = "수면제 및 약물 복용량 (mg)", example = "5.0", minimum = "0", maximum = "100")
    @DecimalMin(value = "0.0", message = "약물 복용량은 0mg 이상이어야 합니다")
    @DecimalMax(value = "100.0", message = "약물 복용량은 100mg을 초과할 수 없습니다")
    private Double medicationMg;

    // === 단순 기록 ===

    @Schema(description = "운동 여부", example = "true")
    @Builder.Default
    private Boolean didExercise = false;

    @Schema(description = "자유 기재 일기", example = "오늘은 스트레스가 많아서 잠들기 어려웠다")
    @Size(max = 1000, message = "일기는 1000자를 초과할 수 없습니다")
    private String diaryNotes;

    // === 주관적 평가 ===

    @Schema(description = "주관적 수면 품질 점수 (1-10)", example = "7", minimum = "1", maximum = "10")
    @Min(value = 1, message = "주관적 수면 품질 점수는 1 이상이어야 합니다")
    @Max(value = 10, message = "주관적 수면 품질 점수는 10 이하여야 합니다")
    private Integer subjectiveSleepQuality;

    @Schema(description = "아침 기상 시 컨디션 점수 (1-10)", example = "6", minimum = "1", maximum = "10")
    @Min(value = 1, message = "아침 컨디션 점수는 1 이상이어야 합니다")
    @Max(value = 10, message = "아침 컨디션 점수는 10 이하여야 합니다")
    private Integer morningConditionScore;

    @Schema(description = "스트레스 레벨 (1-10)", example = "8", minimum = "1", maximum = "10")
    @Min(value = 1, message = "스트레스 레벨은 1 이상이어야 합니다")
    @Max(value = 10, message = "스트레스 레벨은 10 이하여야 합니다")
    private Integer stressLevel;

    @Schema(description = "주관적 졸림 정도 (1-10, 1: 각성, 10: 졸림)", example = "5", minimum = "1", maximum = "10")
    @Min(value = 1, message = "졸림 정도는 1 이상이어야 합니다")
    @Max(value = 10, message = "졸림 정도는 10 이하여야 합니다")
    private Integer sleepiness;

    @Schema(description = "멜라토닌 섭취량 (mg)", example = "3.0", minimum = "0", maximum = "50")
    @DecimalMin(value = "0.0", message = "멜라토닌 섭취량은 0mg 이상이어야 합니다")
    @DecimalMax(value = "50.0", message = "멜라토닌 섭취량은 50mg을 초과할 수 없습니다")
    private Double melatoninIntake;

    // === 비즈니스 로직 검증 메서드 ===

    /**
     * 수면 시간 관련 데이터의 일관성을 검증합니다.
     *
     * @throws IllegalArgumentException 데이터 간 불일치가 있는 경우
     */
    public void validateSleepTimeConsistency() {
        // 각성 시간이 주관적 수면 시간을 초과하는 경우 검증
        if (perceivedSleepMinutes != null && totalAwakeMinutes != null) {
            if (totalAwakeMinutes > perceivedSleepMinutes) {
                throw new IllegalArgumentException("총 각성 시간이 주관적 수면 시간을 초과할 수 없습니다");
            }
        }

        // 침대에 있던 시간과 수면 시간의 합리성 검증
        if (bedTime != null && wakeUpTime != null && perceivedSleepMinutes != null) {
            // 실제 침대 시간 계산 로직은 엔티티에서 처리
        }
    }

    /**
     * 생활습관 데이터의 합리성을 검증합니다.
     */
    public void validateLifestyleConsistency() {
        // 과도한 카페인과 스트레스의 관계성 경고
        if (caffeineMg != null && caffeineMg > 500 && stressLevel != null && stressLevel > 7) {
            // 경고 로그 또는 비즈니스 규칙 적용 (실제로는 서비스 레이어에서 처리)
        }
    }
}