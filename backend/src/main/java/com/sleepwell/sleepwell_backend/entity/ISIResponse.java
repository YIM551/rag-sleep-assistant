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
 * ISI (Insomnia Severity Index) 설문 응답 엔티티
 * 불면증 심각도를 평가하는 설문 응답을 저장
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Entity
@Table(name = "isi_responses", indexes = {
    @Index(name = "idx_isi_user_created", columnList = "user_id,created_at DESC"),
    @Index(name = "idx_isi_user_score", columnList = "user_id,total_score DESC")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ISIResponse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // q1a: 잠들기 어려움 (0-4)
    @Column(name = "q1a", nullable = false)
    private Integer q1a;

    // q1b: 잠을 유지하기 어려움 (0-4)
    @Column(name = "q1b", nullable = false)
    private Integer q1b;

    // q1c: 너무 일찍 깨는 문제 (0-4)
    @Column(name = "q1c", nullable = false)
    private Integer q1c;

    // q2: 현재 수면 패턴에 대한 만족도 (0-4)
    @Column(name = "q2", nullable = false)
    private Integer q2;

    // q3: 수면 문제가 타인에게 얼마나 눈에 띄는지 (0-4)
    @Column(name = "q3", nullable = false)
    private Integer q3;

    // q4: 수면 문제에 대해 얼마나 걱정하는지 (0-4)
    @Column(name = "q4", nullable = false)
    private Integer q4;

    // q5: 수면 문제가 일상생활을 방해하는 정도 (0-4)
    @Column(name = "q5", nullable = false)
    private Integer q5;

    // 총점 (0-28)
    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    // 해석
    @Column(name = "interpretation", nullable = false, length = 100)
    private String interpretation;

    /**
     * 총점 계산
     */
    public Integer calculateTotalScore() {
        return q1a + q1b + q1c + q2 + q3 + q4 + q5;
    }

    /**
     * 점수 기반 해석 생성
     */
    public String generateInterpretation() {
        int total = calculateTotalScore();
        if (total >= 0 && total <= 7) {
            return "불면증 없음";
        } else if (total <= 14) {
            return "경도 불면증";
        } else if (total <= 21) {
            return "중등도 불면증";
        } else if (total <= 28) {
            return "중증 불면증";
        }
        throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
            "ISI 총점이 유효 범위(0-28)를 벗어났습니다: " + total);
    }

    /**
     * 응답 데이터 유효성 검증
     */
    public void validateResponses() {
        validateScore(q1a, "q1a");
        validateScore(q1b, "q1b");
        validateScore(q1c, "q1c");
        validateScore(q2, "q2");
        validateScore(q3, "q3");
        validateScore(q4, "q4");
        validateScore(q5, "q5");

        int calculatedTotal = calculateTotalScore();
        if (calculatedTotal != totalScore) {
            throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                "총점 불일치. 계산값: " + calculatedTotal + ", 저장값: " + totalScore);
        }
    }

    /**
     * 개별 점수 유효성 검증 (0-4 범위)
     */
    private void validateScore(Integer score, String fieldName) {
        if (score == null || score < 0 || score > 4) {
            throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                fieldName + " 점수는 0-4 사이여야 합니다: " + score);
        }
    }
}
