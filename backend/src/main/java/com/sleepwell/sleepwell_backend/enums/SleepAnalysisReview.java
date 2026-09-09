package com.sleepwell.sleepwell_backend.enums;

/**
 * 수면 분석 리뷰 상태
 * 수면 분석 결과에 대한 리뷰 상태를 나타냅니다.
 */
public enum SleepAnalysisReview {
    PENDING("검토 대기"),
    IN_PROGRESS("검토 중"),
    COMPLETED("검토 완료"),
    NEEDS_REVISION("수정 필요"),
    APPROVED("승인됨"),
    REJECTED("거부됨");

    private final String description;

    SleepAnalysisReview(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 