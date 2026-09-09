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
 * BDI-II (Beck Depression Inventory-II) 설문 응답 엔티티
 * 우울 증상을 평가하는 설문 응답을 저장
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Entity
@Table(name = "bdi_responses", indexes = {
    @Index(name = "idx_bdi_user_created", columnList = "user_id,created_at DESC"),
    @Index(name = "idx_bdi_user_score", columnList = "user_id,total_score DESC"),
    @Index(name = "idx_bdi_suicide_risk", columnList = "suicide_risk,created_at DESC")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BDIResponse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // q1-q21: BDI-II 문항 (0-3)
    @Column(name = "q1", nullable = false)
    private Integer q1;

    @Column(name = "q2", nullable = false)
    private Integer q2;

    @Column(name = "q3", nullable = false)
    private Integer q3;

    @Column(name = "q4", nullable = false)
    private Integer q4;

    @Column(name = "q5", nullable = false)
    private Integer q5;

    @Column(name = "q6", nullable = false)
    private Integer q6;

    @Column(name = "q7", nullable = false)
    private Integer q7;

    @Column(name = "q8", nullable = false)
    private Integer q8;

    @Column(name = "q9", nullable = false)
    private Integer q9; // 자살 관련 문항 (중요!)

    @Column(name = "q10", nullable = false)
    private Integer q10;

    @Column(name = "q11", nullable = false)
    private Integer q11;

    @Column(name = "q12", nullable = false)
    private Integer q12;

    @Column(name = "q13", nullable = false)
    private Integer q13;

    @Column(name = "q14", nullable = false)
    private Integer q14;

    @Column(name = "q15", nullable = false)
    private Integer q15;

    @Column(name = "q16", nullable = false)
    private Integer q16;

    @Column(name = "q17", nullable = false)
    private Integer q17;

    @Column(name = "q18", nullable = false)
    private Integer q18;

    @Column(name = "q19", nullable = false)
    private Integer q19;

    @Column(name = "q20", nullable = false)
    private Integer q20;

    @Column(name = "q21", nullable = false)
    private Integer q21;

    // 총점 (0-63)
    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    // 해석
    @Column(name = "interpretation", nullable = false, length = 100)
    private String interpretation;

    /**
     * 자살 위험도 (q9 >= 2인 경우 true)
     * 이 필드는 관리자 모니터링을 위한 중요한 지표
     */
    @Column(name = "suicide_risk", nullable = false)
    private Boolean suicideRisk;

    /**
     * 총점 계산
     */
    public Integer calculateTotalScore() {
        return q1 + q2 + q3 + q4 + q5 + q6 + q7 + q8 + q9 + q10 +
               q11 + q12 + q13 + q14 + q15 + q16 + q17 + q18 + q19 + q20 + q21;
    }

    /**
     * 점수 기반 해석 생성
     */
    public String generateInterpretation() {
        int total = calculateTotalScore();
        if (total >= 0 && total <= 13) {
            return "우울감 최소/없음";
        } else if (total <= 19) {
            return "경도 우울";
        } else if (total <= 28) {
            return "중등도 우울";
        } else if (total <= 63) {
            return "중증 우울";
        }
        throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
            "BDI-II 총점이 유효 범위(0-63)를 벗어났습니다: " + total);
    }

    /**
     * 자살 위험도 판단
     * q9 (자살 사고) >= 2인 경우 true
     */
    public Boolean calculateSuicideRisk() {
        return q9 != null && q9 >= 2;
    }

    /**
     * 응답 데이터 유효성 검증
     */
    public void validateResponses() {
        validateScore(q1, "q1");
        validateScore(q2, "q2");
        validateScore(q3, "q3");
        validateScore(q4, "q4");
        validateScore(q5, "q5");
        validateScore(q6, "q6");
        validateScore(q7, "q7");
        validateScore(q8, "q8");
        validateScore(q9, "q9");
        validateScore(q10, "q10");
        validateScore(q11, "q11");
        validateScore(q12, "q12");
        validateScore(q13, "q13");
        validateScore(q14, "q14");
        validateScore(q15, "q15");
        validateScore(q16, "q16");
        validateScore(q17, "q17");
        validateScore(q18, "q18");
        validateScore(q19, "q19");
        validateScore(q20, "q20");
        validateScore(q21, "q21");

        int calculatedTotal = calculateTotalScore();
        if (calculatedTotal != totalScore) {
            throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                "총점 불일치. 계산값: " + calculatedTotal + ", 저장값: " + totalScore);
        }

        Boolean calculatedRisk = calculateSuicideRisk();
        if (!calculatedRisk.equals(suicideRisk)) {
            throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                "자살 위험도 불일치. 계산값: " + calculatedRisk + ", 저장값: " + suicideRisk);
        }
    }

    /**
     * 개별 점수 유효성 검증 (0-3 범위)
     */
    private void validateScore(Integer score, String fieldName) {
        if (score == null || score < 0 || score > 3) {
            throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                fieldName + " 점수는 0-3 사이여야 합니다: " + score);
        }
    }

    /**
     * 자살 위험도가 높은지 확인
     */
    public boolean isHighSuicideRisk() {
        return suicideRisk;
    }
}
