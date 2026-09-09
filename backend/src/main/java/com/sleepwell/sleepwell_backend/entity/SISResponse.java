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

/**
 * SIS (Sleep Impact Scale) 설문 응답 엔티티
 * 수면 심각도 점수를 평가하는 설문 응답을 저장
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Entity
@Table(name = "sis_responses", indexes = {
    @Index(name = "idx_sis_user_created", columnList = "user_id,created_at DESC"),
    @Index(name = "idx_sis_user_score", columnList = "user_id,total_score DESC")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SISResponse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // ===== Domain 1: Daily Activities (일상 활동) - 8 questions =====
    @Column(name = "q1a", nullable = false)
    private Integer q1a; // 식사 준비

    @Column(name = "q1b", nullable = false)
    private Integer q1b; // 사회 활동

    @Column(name = "q1c", nullable = false)
    private Integer q1c; // 신체 활동

    @Column(name = "q1d", nullable = false)
    private Integer q1d; // 일상 관리

    @Column(name = "q1e", nullable = false)
    private Integer q1e; // 직장/학교 일정

    @Column(name = "q1f", nullable = false)
    private Integer q1f; // 업무/학업 수행

    @Column(name = "q1g", nullable = false)
    private Integer q1g; // 목표 달성

    @Column(name = "q1h", nullable = false)
    private Integer q1h; // 시간 관리

    // ===== Domain 2: Daytime Emotional Impact (주간 정서 영향) - 4 questions =====
    @Column(name = "q2a", nullable = false)
    private Integer q2a; // 짜증/화남

    @Column(name = "q2b", nullable = false)
    private Integer q2b; // 기분 변화

    @Column(name = "q2c", nullable = false)
    private Integer q2c; // 불안/걱정

    @Column(name = "q2d", nullable = false)
    private Integer q2d; // 우울/슬픔

    // ===== Domain 3: Nighttime Emotional Impact (야간 정서 영향) - 5 questions =====
    @Column(name = "q3a", nullable = false)
    private Integer q3a; // 수면 관련 좌절

    @Column(name = "q3b", nullable = false)
    private Integer q3b; // 수면 관련 걱정

    @Column(name = "q3c", nullable = false)
    private Integer q3c; // 낮 일에 영향 두려움

    @Column(name = "q3d", nullable = false)
    private Integer q3d; // 수면 패턴 불만족

    @Column(name = "q3e", nullable = false)
    private Integer q3e; // 수면 문제 통제 불능감

    // ===== Domain 4: Fatigue (피로) - 7 questions =====
    @Column(name = "q4a", nullable = false)
    private Integer q4a; // 아침 피로감

    @Column(name = "q4b", nullable = false)
    private Integer q4b; // 오후 피로감

    @Column(name = "q4c", nullable = false)
    private Integer q4c; // 에너지 부족

    @Column(name = "q4d", nullable = false)
    private Integer q4d; // 탈진 느낌

    @Column(name = "q4e", nullable = false)
    private Integer q4e; // 신체적 지침

    @Column(name = "q4f", nullable = false)
    private Integer q4f; // 정신적 지침

    @Column(name = "q4g", nullable = false)
    private Integer q4g; // 하루 종일 피로

    // ===== Domain 5: Social Impact (사회적 영향) - 4 questions =====
    @Column(name = "q5a", nullable = false)
    private Integer q5a; // 가족 관계

    @Column(name = "q5b", nullable = false)
    private Integer q5b; // 친구 관계

    @Column(name = "q5c", nullable = false)
    private Integer q5c; // 사회적 활동 참여

    @Column(name = "q5d", nullable = false)
    private Integer q5d; // 타인과 상호작용

    // ===== Domain 6: Mental Fatigue (정신적 피로) - 4 questions =====
    @Column(name = "q6a", nullable = false)
    private Integer q6a; // 집중력

    @Column(name = "q6b", nullable = false)
    private Integer q6b; // 기억력

    @Column(name = "q6c", nullable = false)
    private Integer q6c; // 명확한 사고

    @Column(name = "q6d", nullable = false)
    private Integer q6d; // 문제 해결 능력

    // ===== Domain 7: Sleep Satisfaction (수면 만족도) - 3 questions =====
    @Column(name = "q7a", nullable = false)
    private Integer q7a; // 수면의 질

    @Column(name = "q7b", nullable = false)
    private Integer q7b; // 수면량

    @Column(name = "q7c", nullable = false)
    private Integer q7c; // 전반적 수면 만족도

    // 총점 (35-175)
    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    // 해석
    @Column(name = "interpretation", nullable = false, length = 100)
    private String interpretation;

    /**
     * 총점 계산
     */
    public Integer calculateTotalScore() {
        return q1a + q1b + q1c + q1d + q1e + q1f + q1g + q1h +
               q2a + q2b + q2c + q2d +
               q3a + q3b + q3c + q3d + q3e +
               q4a + q4b + q4c + q4d + q4e + q4f + q4g +
               q5a + q5b + q5c + q5d +
               q6a + q6b + q6c + q6d +
               q7a + q7b + q7c;
    }

    /**
     * 점수 기반 해석 생성
     */
    public String generateInterpretation() {
        int total = calculateTotalScore();
        if (total >= 35 && total <= 70) {
            return "경미한 수면 영향";
        } else if (total <= 105) {
            return "중등도 수면 영향";
        } else if (total <= 140) {
            return "심각한 수면 영향";
        } else if (total <= 175) {
            return "매우 심각한 수면 영향";
        }
        throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
            "SIS 총점이 유효 범위(35-175)를 벗어났습니다: " + total);
    }

    /**
     * 응답 데이터 유효성 검증
     */
    public void validateResponses() {
        // Domain 1: Daily Activities
        validateScore(q1a, "q1a");
        validateScore(q1b, "q1b");
        validateScore(q1c, "q1c");
        validateScore(q1d, "q1d");
        validateScore(q1e, "q1e");
        validateScore(q1f, "q1f");
        validateScore(q1g, "q1g");
        validateScore(q1h, "q1h");

        // Domain 2: Daytime Emotional Impact
        validateScore(q2a, "q2a");
        validateScore(q2b, "q2b");
        validateScore(q2c, "q2c");
        validateScore(q2d, "q2d");

        // Domain 3: Nighttime Emotional Impact
        validateScore(q3a, "q3a");
        validateScore(q3b, "q3b");
        validateScore(q3c, "q3c");
        validateScore(q3d, "q3d");
        validateScore(q3e, "q3e");

        // Domain 4: Fatigue
        validateScore(q4a, "q4a");
        validateScore(q4b, "q4b");
        validateScore(q4c, "q4c");
        validateScore(q4d, "q4d");
        validateScore(q4e, "q4e");
        validateScore(q4f, "q4f");
        validateScore(q4g, "q4g");

        // Domain 5: Social Impact
        validateScore(q5a, "q5a");
        validateScore(q5b, "q5b");
        validateScore(q5c, "q5c");
        validateScore(q5d, "q5d");

        // Domain 6: Mental Fatigue
        validateScore(q6a, "q6a");
        validateScore(q6b, "q6b");
        validateScore(q6c, "q6c");
        validateScore(q6d, "q6d");

        // Domain 7: Sleep Satisfaction
        validateScore(q7a, "q7a");
        validateScore(q7b, "q7b");
        validateScore(q7c, "q7c");

        int calculatedTotal = calculateTotalScore();
        if (calculatedTotal != totalScore) {
            throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                "총점 불일치. 계산값: " + calculatedTotal + ", 저장값: " + totalScore);
        }
    }

    /**
     * 개별 점수 유효성 검증 (1-5 범위)
     */
    private void validateScore(Integer score, String fieldName) {
        if (score == null || score < 1 || score > 5) {
            throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                fieldName + " 점수는 1-5 사이여야 합니다: " + score);
        }
    }
}
