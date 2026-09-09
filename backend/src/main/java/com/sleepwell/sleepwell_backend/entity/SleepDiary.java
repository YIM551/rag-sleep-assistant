package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * 수면일지 엔티티 (SleepDiary Entity)
 *
 * 사용자가 직접 입력하는 주관적인 수면 관련 정보를 저장하고 관리하는 엔티티입니다.
 * 기존의 SleepRecord가 웨어러블 기기에서 수집된 객관적 데이터를 저장한다면,
 * SleepDiary는 사용자의 주관적인 경험과 생활습관 정보를 기록합니다.
 *
 * 주요 기능:
 * - 사용자의 주관적 수면 경험 기록
 * - 생활습관 및 환경 요소 추적 (카페인, 알코올, 운동 등)
 * - AI 분석을 위한 맥락 정보 제공
 * - 수면 패턴과 생활습관의 상관관계 분석
 *
 * 데이터 분류:
 * 1. 수치화 기록 (그래프화 가능): 낮잠 시간, 카페인 섭취량, 알코올 섭취량 등
 * 2. 단순 기록 (Yes/No 또는 텍스트): 운동 여부, 자유 일기
 *
 * 설계 원칙:
 * - null 대신 Optional 반환으로 null-safe 코드 작성
 * - 엔티티는 도메인 로직에만 집중 (JSON 직렬화 관심사 분리)
 * - BusinessException으로 일관된 예외 처리
 * - 불변성과 캡슐화 보장
 *
 * @author SleepWell Development Team
 * @since 1.0
 * @see User
 * @see SleepRecord
 * @see BaseEntity
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    name = "sleep_diary",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_SLEEP_DIARY_USER_DATE",
            columnNames = {"user_id", "diary_date"}
        )
    },
    indexes = {
        // 사용자별 최신 일지 조회 (메인 대시보드)
        @Index(name = "IDX_SLEEP_DIARY_USER_DATE", columnList = "user_id, diary_date"),
        // 기간별 일지 조회 (트렌드 분석)
        @Index(name = "IDX_SLEEP_DIARY_DATE_RANGE", columnList = "diary_date, user_id"),
        // 카페인 섭취량 분석용
        @Index(name = "IDX_SLEEP_DIARY_CAFFEINE", columnList = "caffeine_mg, diary_date"),
        // 알코올 섭취량 분석용
        @Index(name = "IDX_SLEEP_DIARY_ALCOHOL", columnList = "alcohol_ml, diary_date"),
        // 운동 여부별 분석
        @Index(name = "IDX_SLEEP_DIARY_EXERCISE", columnList = "did_exercise, diary_date")
    }
)
public class SleepDiary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 일지 작성자
     * 김영한 스타일: 연관관계 매핑은 엔티티에서, JSON 직렬화는 DTO에서 처리
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_SLEEP_DIARY_USER"))
    private User user;

    /**
     * 일지 날짜 (고유 식별을 위한 날짜)
     */
    @Column(name = "diary_date", nullable = false)
    private LocalDate diaryDate;

    // === 수치화 기록 (그래프화 가능) ===

    /**
     * 낮잠 시간 (분)
     * 사용자가 하루 동안 낮잠을 잔 총 시간
     */
    @Column(name = "nap_minutes")
    private Integer napMinutes;

    /**
     * 침대에 누운 시간
     * 사용자가 실제 잠들기 위해 침대에 누운 시간
     */
    @Column(name = "bed_time")
    private LocalTime bedTime;

    /**
     * 주관적 수면 시간 (분)
     * 사용자가 실제로 잤다고 생각하는 시간 (주관적 판단)
     */
    @Column(name = "perceived_sleep_minutes")
    private Integer perceivedSleepMinutes;

    /**
     * 각성 횟수
     * 수면 중 깨어난 횟수 (사용자 기억 기준)
     */
    @Column(name = "awakening_count")
    private Integer awakeningCount;

    /**
     * 총 각성 시간 (분)
     * 수면 중 깨어있던 총 시간
     */
    @Column(name = "total_awake_minutes")
    private Integer totalAwakeMinutes;

    /**
     * 침대에서 일어난 시간
     * 최종적으로 잠자리에서 일어난 시간
     */
    @Column(name = "wake_up_time")
    private LocalTime wakeUpTime;

    /**
     * 카페인 섭취량 (mg)
     * 하루 동안 섭취한 총 카페인량
     */
    @Column(name = "caffeine_mg")
    private Integer caffeineMg;

    /**
     * 알코올 섭취량 (ml)
     * 하루 동안 섭취한 총 알코올량
     */
    @Column(name = "alcohol_ml")
    private Integer alcoholMl;

    /**
     * 수면제 및 약물 복용량 (mg)
     * 수면에 영향을 줄 수 있는 약물의 총 복용량
     */
    @Column(name = "medication_mg")
    private Double medicationMg;

    // === 단순 기록 (Yes/No 또는 텍스트) ===

    /**
     * 운동 여부
     * 하루 동안 운동을 했는지 여부
     */
    @Column(name = "did_exercise", nullable = false)
    @Builder.Default
    private Boolean didExercise = false;

    /**
     * 자유 기재 일기
     * 사용자가 자유롭게 작성하는 수면 관련 메모
     */
    @Column(name = "diary_notes", columnDefinition = "TEXT")
    private String diaryNotes;

    // === 추가 주관적 평가 ===

    /**
     * 주관적 수면 품질 점수 (1-10)
     * 사용자가 평가하는 수면의 질
     */
    @Column(name = "subjective_sleep_quality")
    private Integer subjectiveSleepQuality;

    /**
     * 아침 기상 시 컨디션 점수 (1-10)
     * 기상 시 느낀 컨디션 상태
     */
    @Column(name = "morning_condition_score")
    private Integer morningConditionScore;

    /**
     * 스트레스 레벨 (1-10)
     * 해당 일의 스트레스 정도
     */
    @Column(name = "stress_level")
    private Integer stressLevel;

    /**
     * 주관적 졸림 정도 (1-10)
     * 1: 각성 상태, 10: 매우 졸림
     */
    @Column(name = "sleepiness")
    private Integer sleepiness;

    /**
     * 멜라토닌 섭취량 (mg)
     * 하루 동안 섭취한 총 멜라토닌 양
     */
    @Column(name = "melatonin_intake_mg")
    private Double melatoninIntake;

    // === 도메인 로직 메서드 (김영한 스타일: Optional 반환) ===

    /**
     * 총 침상 시간 계산 (분)
     * 침대에 누운 시간부터 일어난 시간까지의 총 시간
     *
     * @return 계산된 침상 시간, 필요한 데이터가 없으면 Optional.empty()
     */
    public Optional<Integer> calculateTotalBedTime() {
        if (bedTime == null || wakeUpTime == null) {
            return Optional.empty();
        }

        // 자정을 넘어가는 경우 처리
        if (wakeUpTime.isBefore(bedTime)) {
            // 다음 날 아침에 일어나는 경우 (23:30 -> 07:00)
            // 23:30 -> 00:00까지 30분, 00:00 -> 07:00까지 420분
            long minutesToMidnight = (24 * 60) - (bedTime.getHour() * 60 + bedTime.getMinute());
            long minutesFromMidnight = wakeUpTime.getHour() * 60 + wakeUpTime.getMinute();
            return Optional.of((int) (minutesToMidnight + minutesFromMidnight));
        } else {
            // 같은 날 내에서 잠깐 잔 경우 (낮잠 등)
            long minutes = ChronoUnit.MINUTES.between(bedTime, wakeUpTime);
            return Optional.of((int) minutes);
        }
    }

    /**
     * 주관적 수면 효율성 계산 (%)
     * 주관적 수면시간 / 총 침상시간 * 100
     *
     * @return 수면 효율성 퍼센트, 계산할 수 없으면 Optional.empty()
     */
    public Optional<Double> calculateSubjectiveSleepEfficiency() {
        return calculateTotalBedTime()
            .filter(bedTime -> bedTime > 0)
            .filter(bedTime -> perceivedSleepMinutes != null && perceivedSleepMinutes > 0)
            .map(bedTime -> (double) perceivedSleepMinutes / bedTime * 100);
    }

    /**
     * 카페인 섭취 레벨 분류
     * @return NONE(0mg), LOW(1-100mg), MODERATE(101-300mg), HIGH(300mg+)
     */
    public CaffeineIntakeLevel getCaffeineIntakeLevel() {
        if (caffeineMg == null || caffeineMg == 0) {
            return CaffeineIntakeLevel.NONE;
        } else if (caffeineMg <= 100) {
            return CaffeineIntakeLevel.LOW;
        } else if (caffeineMg <= 300) {
            return CaffeineIntakeLevel.MODERATE;
        } else {
            return CaffeineIntakeLevel.HIGH;
        }
    }

    /**
     * 알코올 섭취 레벨 분류
     * @return NONE(0ml), LIGHT(1-200ml), MODERATE(201-400ml), HEAVY(400ml+)
     */
    public AlcoholIntakeLevel getAlcoholIntakeLevel() {
        if (alcoholMl == null || alcoholMl == 0) {
            return AlcoholIntakeLevel.NONE;
        } else if (alcoholMl <= 200) {
            return AlcoholIntakeLevel.LIGHT;
        } else if (alcoholMl <= 400) {
            return AlcoholIntakeLevel.MODERATE;
        } else {
            return AlcoholIntakeLevel.HEAVY;
        }
    }

    /**
     * 종합 생활습관 점수 계산 (0-100)
     * 운동, 카페인, 알코올, 스트레스를 종합한 점수
     *
     * @return 생활습관 점수 (0-100 범위 보장)
     */
    public double calculateLifestyleScore() {
        double score = 50.0; // 기본 점수

        // 운동 여부 (+10점)
        if (Boolean.TRUE.equals(didExercise)) {
            score += 10;
        }

        // 카페인 섭취량에 따른 점수 조정
        if (caffeineMg != null) {
            switch (getCaffeineIntakeLevel()) {
                case NONE -> score += 10;      // 카페인 안 마심
                case LOW -> score += 5;       // 적당한 카페인
                case MODERATE -> { /* 점수 변화 없음 */ }
                case HIGH -> score -= 10;     // 과다 카페인
            }
        }

        // 알코올 섭취량에 따른 점수 조정
        if (alcoholMl != null) {
            switch (getAlcoholIntakeLevel()) {
                case NONE -> score += 10;     // 금주
                case LIGHT -> score -= 5;     // 적당한 음주
                case MODERATE -> score -= 10; // 보통 음주
                case HEAVY -> score -= 15;    // 과음
            }
        }

        // 스트레스 레벨에 따른 점수 조정 (5를 기준으로)
        if (stressLevel != null) {
            score += (5 - stressLevel) * 2.0;  // 스트레스가 낮을수록 점수 증가
        }

        return Math.max(0, Math.min(100, score)); // 0-100 범위 제한
    }

    /**
     * 수면일지와 연관된 객관적 수면기록 찾기
     * 같은 날짜의 SleepRecord를 찾아 연관성 분석에 활용
     */
    public boolean hasSameDate(SleepRecord sleepRecord) {
        return Objects.equals(this.diaryDate, sleepRecord.getRecordDate());
    }

    // === 도메인 규칙을 적용한 상태 변경 메서드 ===

    /**
     * 일지 날짜 설정 (도메인 규칙 적용)
     */
    public void changeDiaryDate(LocalDate newDate) {
        if (newDate == null) {
            throw new BusinessException("일지 날짜는 null일 수 없습니다", HttpStatus.BAD_REQUEST, "INVALID_DIARY_DATE");
        }
        if (newDate.isAfter(LocalDate.now())) {
            throw new BusinessException("일지 날짜는 미래 날짜일 수 없습니다", HttpStatus.BAD_REQUEST, "FUTURE_DIARY_DATE");
        }
        this.diaryDate = newDate;
    }

    /**
     * 주관적 수면 품질 점수 설정 (도메인 규칙 검증)
     */
    public void changeSubjectiveSleepQuality(Integer score) {
        if (score != null && (score < 1 || score > 10)) {
            throw new BusinessException("주관적 수면 품질 점수는 1-10 사이여야 합니다", HttpStatus.BAD_REQUEST, "INVALID_SLEEP_QUALITY_SCORE");
        }
        this.subjectiveSleepQuality = score;
    }

    /**
     * 아침 컨디션 점수 설정 (도메인 규칙 검증)
     */
    public void changeMorningConditionScore(Integer score) {
        if (score != null && (score < 1 || score > 10)) {
            throw new BusinessException("아침 컨디션 점수는 1-10 사이여야 합니다", HttpStatus.BAD_REQUEST, "INVALID_MORNING_CONDITION_SCORE");
        }
        this.morningConditionScore = score;
    }

    /**
     * 스트레스 레벨 설정 (도메인 규칙 검증)
     */
    public void changeStressLevel(Integer level) {
        if (level != null && (level < 1 || level > 10)) {
            throw new BusinessException("스트레스 레벨은 1-10 사이여야 합니다", HttpStatus.BAD_REQUEST, "INVALID_STRESS_LEVEL");
        }
        this.stressLevel = level;
    }

    /**
     * 카페인 섭취량 설정 (도메인 규칙 검증)
     */
    public void changeCaffeineIntake(Integer mg) {
        if (mg != null && mg < 0) {
            throw new BusinessException("카페인 섭취량은 0 이상이어야 합니다", HttpStatus.BAD_REQUEST, "INVALID_CAFFEINE_AMOUNT");
        }
        this.caffeineMg = mg;
    }

    /**
     * 알코올 섭취량 설정 (도메인 규칙 검증)
     */
    public void changeAlcoholIntake(Integer ml) {
        if (ml != null && ml < 0) {
            throw new BusinessException("알코올 섭취량은 0 이상이어야 합니다", HttpStatus.BAD_REQUEST, "INVALID_ALCOHOL_AMOUNT");
        }
        this.alcoholMl = ml;
    }

    /**
     * 졸림 정도 설정 (도메인 규칙 검증)
     */
    public void changeSleepiness(Integer level) {
        if (level != null && (level < 1 || level > 10)) {
            throw new BusinessException("졸림 정도는 1-10 사이여야 합니다", HttpStatus.BAD_REQUEST, "INVALID_SLEEPINESS_LEVEL");
        }
        this.sleepiness = level;
    }

    /**
     * 멜라토닌 섭취량 설정 (도메인 규칙 검증)
     */
    public void changeMelatoninIntake(Double mg) {
        if (mg != null && (mg < 0 || mg > 50)) {
            throw new BusinessException("멜라토닌 섭취량은 0-50mg 사이여야 합니다", HttpStatus.BAD_REQUEST, "INVALID_MELATONIN_AMOUNT");
        }
        this.melatoninIntake = mg;
    }

    // === 연관관계 편의 메서드 (김영한 스타일) ===

    /**
     * 사용자 설정 (연관관계 편의 메서드)
     */
    public void setUser(User user) {
        this.user = user;
    }

    // === 내부 Enum 클래스 ===

    /**
     * 카페인 섭취 레벨 분류
     */
    public enum CaffeineIntakeLevel {
        NONE("카페인 없음"),
        LOW("낮음 (1-100mg)"),
        MODERATE("보통 (101-300mg)"),
        HIGH("높음 (300mg+)");

        private final String description;

        CaffeineIntakeLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 알코올 섭취 레벨 분류
     */
    public enum AlcoholIntakeLevel {
        NONE("금주"),
        LIGHT("가벼운 음주 (1-200ml)"),
        MODERATE("보통 음주 (201-400ml)"),
        HEAVY("과음 (400ml+)");

        private final String description;

        AlcoholIntakeLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // === 도메인 메서드 ===

    /**
     * 수면일지 정보 업데이트 (null-safe 업데이트)
     * null이나 빈 문자열이 아닌 필드만 업데이트하고, null/빈 문자열인 필드는 기존 값 유지
     *
     * @param dto 업데이트할 수면일지 정보
     */
    public void updateDiary(com.sleepwell.sleepwell_backend.dto.SleepDiaryRequestDto dto) {
        if (dto.getNapMinutes() != null) {
            this.napMinutes = dto.getNapMinutes();
        }
        if (dto.getBedTime() != null) {
            this.bedTime = dto.getBedTime();
        }
        if (dto.getPerceivedSleepMinutes() != null) {
            this.perceivedSleepMinutes = dto.getPerceivedSleepMinutes();
        }
        if (dto.getAwakeningCount() != null) {
            this.awakeningCount = dto.getAwakeningCount();
        }
        if (dto.getTotalAwakeMinutes() != null) {
            this.totalAwakeMinutes = dto.getTotalAwakeMinutes();
        }
        if (dto.getWakeUpTime() != null) {
            this.wakeUpTime = dto.getWakeUpTime();
        }
        if (dto.getCaffeineMg() != null) {
            this.caffeineMg = dto.getCaffeineMg();
        }
        if (dto.getAlcoholMl() != null) {
            this.alcoholMl = dto.getAlcoholMl();
        }
        if (dto.getMedicationMg() != null) {
            this.medicationMg = dto.getMedicationMg();
        }
        if (dto.getDidExercise() != null) {
            this.didExercise = dto.getDidExercise();
        }
        // 문자열 필드는 빈 문자열도 체크
        if (isNotBlank(dto.getDiaryNotes())) {
            this.diaryNotes = dto.getDiaryNotes();
        }
        if (dto.getSubjectiveSleepQuality() != null) {
            this.subjectiveSleepQuality = dto.getSubjectiveSleepQuality();
        }
        if (dto.getMorningConditionScore() != null) {
            this.morningConditionScore = dto.getMorningConditionScore();
        }
        if (dto.getStressLevel() != null) {
            this.stressLevel = dto.getStressLevel();
        }
        if (dto.getSleepiness() != null) {
            this.sleepiness = dto.getSleepiness();
        }
        if (dto.getMelatoninIntake() != null) {
            this.melatoninIntake = dto.getMelatoninIntake();
        }
    }

    /**
     * 문자열이 null이 아니고 빈 문자열도 아닌지 확인
     *
     * @param value 확인할 문자열
     * @return null이 아니고 trim 후 빈 문자열이 아니면 true
     */
    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}