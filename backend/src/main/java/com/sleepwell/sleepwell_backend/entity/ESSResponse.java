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
 * ESS (Epworth Sleepiness Scale) 설문 응답 엔티티
 * 주간 졸림증을 평가하는 설문 응답을 저장
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Entity
@Table(name = "ess_responses", indexes = {
    @Index(name = "idx_ess_user_created", columnList = "user_id,created_at DESC"),
    @Index(name = "idx_ess_user_score", columnList = "user_id,total_score DESC")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ESSResponse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // q1: 앉아서 책을 읽을 때 (0-3)
    @Column(name = "q1", nullable = false)
    private Integer q1;

    // q2: TV를 볼 때 (0-3)
    @Column(name = "q2", nullable = false)
    private Integer q2;

    // q3: 공공장소에서 가만히 앉아있을 때 (0-3)
    @Column(name = "q3", nullable = false)
    private Integer q3;

    // q4: 1시간 동안 쉬지 않고 차에 승객으로 앉아있을 때 (0-3)
    @Column(name = "q4", nullable = false)
    private Integer q4;

    // q5: 오후에 쉬기 위해 누웠을 때 (0-3)
    @Column(name = "q5", nullable = false)
    private Integer q5;

    // q6: 앉아서 누군가와 이야기할 때 (0-3)
    @Column(name = "q6", nullable = false)
    private Integer q6;

    // q7: 점심 식사 후 조용히 앉아있을 때 (0-3)
    @Column(name = "q7", nullable = false)
    private Integer q7;

    // q8: 차 안에서 정체 중 몇 분간 멈춰있을 때 (0-3)
    @Column(name = "q8", nullable = false)
    private Integer q8;

    // 총점 (0-24)
    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    // 해석
    @Column(name = "interpretation", nullable = false, length = 100)
    private String interpretation;

    /**
     * 총점 계산
     */
    public Integer calculateTotalScore() {
        return q1 + q2 + q3 + q4 + q5 + q6 + q7 + q8;
    }

    /**
     * 점수 기반 해석 생성
     */
    public String generateInterpretation() {
        int total = calculateTotalScore();
        if (total >= 0 && total <= 10) {
            return "정상 범위";
        } else if (total <= 15) {
            return "경도 졸림증";
        } else if (total <= 24) {
            return "중등도 이상 졸림증";
        }
        throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
            "ESS 총점이 유효 범위(0-24)를 벗어났습니다: " + total);
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

        int calculatedTotal = calculateTotalScore();
        if (calculatedTotal != totalScore) {
            throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                "총점 불일치. 계산값: " + calculatedTotal + ", 저장값: " + totalScore);
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
}
