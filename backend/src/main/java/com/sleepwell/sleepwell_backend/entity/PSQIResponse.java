package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * PSQI (Pittsburgh Sleep Quality Index) 설문 응답 엔티티
 * 수면의 질을 평가하는 설문 응답을 저장
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Entity
@Table(name = "psqi_responses", indexes = {
    @Index(name = "idx_psqi_user_created", columnList = "user_id,created_at DESC"),
    @Index(name = "idx_psqi_user_score", columnList = "user_id,total_score DESC")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PSQIResponse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // q1: 취침 시간 (HH:MM 형식 문자열)
    @Column(name = "q1_bedtime", nullable = false, length = 5)
    private String q1Bedtime;

    // q2: 잠들기까지 걸린 시간 (분)
    @Column(name = "q2_h", nullable = false)
    private Integer q2H;

    @Column(name = "q2_m", nullable = false)
    private Integer q2M;

    // q3: 기상 시간 (HH:MM 형식 문자열)
    @Column(name = "q3_waketime", nullable = false, length = 5)
    private String q3Waketime;

    // q4: 실제 수면 시간
    @Column(name = "q4_h", nullable = false)
    private Integer q4H;

    @Column(name = "q4_m", nullable = false)
    private Integer q4M;

    // q5a-j: 수면 문제 빈도 (0-3)
    @Column(name = "q5a", nullable = false)
    private Integer q5a;

    @Column(name = "q5b", nullable = false)
    private Integer q5b;

    @Column(name = "q5c", nullable = false)
    private Integer q5c;

    @Column(name = "q5d", nullable = false)
    private Integer q5d;

    @Column(name = "q5e", nullable = false)
    private Integer q5e;

    @Column(name = "q5f", nullable = false)
    private Integer q5f;

    @Column(name = "q5g", nullable = false)
    private Integer q5g;

    @Column(name = "q5h", nullable = false)
    private Integer q5h;

    @Column(name = "q5i", nullable = false)
    private Integer q5i;

    @Column(name = "q5j", nullable = false)
    private Integer q5j;

    // q5j 기타 이유 (TEXT)
    @Column(name = "q5j_reason", columnDefinition = "TEXT")
    private String q5jReason;

    // q6: 전반적인 수면의 질 (0-3)
    @Column(name = "q6", nullable = false)
    private Integer q6;

    // q7: 수면제 사용 빈도 (0-3)
    @Column(name = "q7", nullable = false)
    private Integer q7;

    // q8: 깨어있는데 어려움 (0-3)
    @Column(name = "q8", nullable = false)
    private Integer q8;

    // q9: 열정 유지 어려움 (0-3)
    @Column(name = "q9", nullable = false)
    private Integer q9;

    // q10a-e: 동침자/룸메이트 관련
    @Column(name = "q10a", nullable = false)
    private Integer q10a;

    @Column(name = "q10b", nullable = false)
    private Integer q10b;

    @Column(name = "q10c", nullable = false)
    private Integer q10c;

    @Column(name = "q10d", nullable = false)
    private Integer q10d;

    @Column(name = "q10e", nullable = false)
    private Integer q10e;

    // q10e 기타 이유 (TEXT)
    @Column(name = "q10e_detail", columnDefinition = "TEXT")
    private String q10eDetail;

    // PSQI 7개 Component Scores
    @Column(name = "component1_quality", nullable = false)
    private Integer component1Quality;

    @Column(name = "component2_latency", nullable = false)
    private Integer component2Latency;

    @Column(name = "component3_duration", nullable = false)
    private Integer component3Duration;

    @Column(name = "component4_efficiency", nullable = false)
    private Integer component4Efficiency;

    @Column(name = "component5_disturbance", nullable = false)
    private Integer component5Disturbance;

    @Column(name = "component6_medication", nullable = false)
    private Integer component6Medication;

    @Column(name = "component7_dysfunction", nullable = false)
    private Integer component7Dysfunction;

    // 총점 (0-21) - 7개 컴포넌트의 합
    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    // 해석
    @Column(name = "interpretation", nullable = false, length = 100)
    private String interpretation;

    /**
     * Component 1: 주관적 수면의 질 (q6)
     */
    public Integer calculateComponent1() {
        return q6; // 0-3 점수 그대로 사용
    }

    /**
     * Component 2: 수면 잠복기 (q2 + q5a)
     */
    public Integer calculateComponent2() {
        int q2Minutes = (q2H * 60) + q2M;
        int q2Score = 0;
        if (q2Minutes <= 15) q2Score = 0;
        else if (q2Minutes <= 30) q2Score = 1;
        else if (q2Minutes <= 60) q2Score = 2;
        else q2Score = 3;

        int sum = q2Score + q5a;
        if (sum == 0) return 0;
        else if (sum <= 2) return 1;
        else if (sum <= 4) return 2;
        else return 3;
    }

    /**
     * Component 3: 수면 시간 (q4)
     */
    public Integer calculateComponent3() {
        int totalMinutes = (q4H * 60) + q4M;
        if (totalMinutes > 420) return 0; // >7시간
        else if (totalMinutes >= 360) return 1; // 6-7시간
        else if (totalMinutes >= 300) return 2; // 5-6시간
        else return 3; // <5시간
    }

    /**
     * Component 4: 수면 효율 (실제 수면 시간 / 침대 시간 * 100)
     */
    public Integer calculateComponent4() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime bedtime = LocalTime.parse(q1Bedtime, formatter);
            LocalTime waketime = LocalTime.parse(q3Waketime, formatter);

            long minutesInBed = ChronoUnit.MINUTES.between(bedtime, waketime);
            if (minutesInBed < 0) {
                minutesInBed += 24 * 60; // 다음 날로 넘어간 경우
            }

            int actualSleepMinutes = (q4H * 60) + q4M;
            double efficiency = (actualSleepMinutes / (double) minutesInBed) * 100;

            if (efficiency >= 85) return 0;
            else if (efficiency >= 75) return 1;
            else if (efficiency >= 65) return 2;
            else return 3;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                "PSQI 수면 효율 계산 실패: 잘못된 시간 형식");
        }
    }

    /**
     * Component 5: 수면 방해 (q5b-j의 합)
     */
    public Integer calculateComponent5() {
        int sum = q5b + q5c + q5d + q5e + q5f + q5g + q5h + q5i + q5j;
        if (sum == 0) return 0;
        else if (sum <= 9) return 1;
        else if (sum <= 18) return 2;
        else return 3;
    }

    /**
     * Component 6: 수면제 사용 (q7)
     */
    public Integer calculateComponent6() {
        return q7; // 0-3 점수 그대로 사용
    }

    /**
     * Component 7: 주간 기능장애 (q8 + q9)
     */
    public Integer calculateComponent7() {
        int sum = q8 + q9;
        if (sum == 0) return 0;
        else if (sum <= 2) return 1;
        else if (sum <= 4) return 2;
        else return 3;
    }

    /**
     * 총점 계산 (7개 컴포넌트의 합)
     */
    public Integer calculateTotalScore() {
        return component1Quality + component2Latency + component3Duration +
               component4Efficiency + component5Disturbance + component6Medication +
               component7Dysfunction;
    }

    /**
     * 점수 기반 해석 생성
     */
    public String generateInterpretation() {
        int total = calculateTotalScore();
        if (total >= 0 && total <= 5) {
            return "수면의 질 양호";
        } else if (total <= 21) {
            return "수면의 질 불량";
        }
        throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
            "PSQI 총점이 유효 범위(0-21)를 벗어났습니다: " + total);
    }

    /**
     * 응답 데이터 유효성 검증
     */
    public void validateResponses() {
        validateTimeFormat(q1Bedtime, "q1_bedtime");
        validateTimeFormat(q3Waketime, "q3_waketime");

        validateScore(q5a, "q5a", 0, 3);
        validateScore(q5b, "q5b", 0, 3);
        validateScore(q5c, "q5c", 0, 3);
        validateScore(q5d, "q5d", 0, 3);
        validateScore(q5e, "q5e", 0, 3);
        validateScore(q5f, "q5f", 0, 3);
        validateScore(q5g, "q5g", 0, 3);
        validateScore(q5h, "q5h", 0, 3);
        validateScore(q5i, "q5i", 0, 3);
        validateScore(q5j, "q5j", 0, 3);
        validateScore(q6, "q6", 0, 3);
        validateScore(q7, "q7", 0, 3);
        validateScore(q8, "q8", 0, 3);
        validateScore(q9, "q9", 0, 3);

        int calculatedTotal = calculateTotalScore();
        if (calculatedTotal != totalScore) {
            throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                "총점 불일치. 계산값: " + calculatedTotal + ", 저장값: " + totalScore);
        }
    }

    /**
     * 시간 형식 유효성 검증 (HH:MM)
     */
    private void validateTimeFormat(String time, String fieldName) {
        if (time == null || !time.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                fieldName + "은 HH:MM 형식이어야 합니다: " + time);
        }
    }

    /**
     * 개별 점수 유효성 검증
     */
    private void validateScore(Integer score, String fieldName, int min, int max) {
        if (score == null || score < min || score > max) {
            throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                fieldName + " 점수는 " + min + "-" + max + " 사이여야 합니다: " + score);
        }
    }
}
