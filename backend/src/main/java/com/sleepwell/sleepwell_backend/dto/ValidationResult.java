package com.sleepwell.sleepwell_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 데이터 유효성 검증 결과
 * 
 * 플랫폼 데이터의 유효성 검증 결과를 담는 클래스입니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * 유효성 검증 통과 여부
     */
    private boolean valid;

    /**
     * 검증 오류 메시지 목록
     */
    private List<String> errorMessages;

    /**
     * 경고 메시지 목록
     */
    private List<String> warningMessages;

    /**
     * 검증된 필드 수
     */
    private int validatedFieldsCount;

    /**
     * 실패한 필드 수
     */
    private int failedFieldsCount;

    /**
     * 전체 검증 점수 (0-100)
     */
    private int validationScore;

    /**
     * 검증 결과 상세 정보
     */
    private String details;

    /**
     * 성공적인 검증 결과 생성
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .validationScore(100)
                .build();
    }

    /**
     * 실패한 검증 결과 생성
     */
    public static ValidationResult failure(List<String> errors) {
        return ValidationResult.builder()
                .valid(false)
                .errorMessages(errors)
                .validationScore(0)
                .build();
    }
} 