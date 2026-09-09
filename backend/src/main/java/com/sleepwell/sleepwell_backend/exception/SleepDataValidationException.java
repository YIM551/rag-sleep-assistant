package com.sleepwell.sleepwell_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * 수면 데이터 검증 관련 예외 클래스
 * 
 * 수면 데이터의 논리적 검증, 시간 순서, 데이터 정합성 등을 처리합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
public class SleepDataValidationException extends BusinessException {

    public SleepDataValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "SLEEP_DATA_VALIDATION_ERROR");
    }

    public SleepDataValidationException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }

    public SleepDataValidationException(String message, HttpStatus status, String errorCode) {
        super(message, status, errorCode);
    }

    // 수면 시간 순서 오류
    public static SleepDataValidationException invalidSleepTimeOrder() {
        return new SleepDataValidationException(
            "잠든 시간이 깬 시간보다 늦을 수 없습니다", 
            "INVALID_SLEEP_TIME_ORDER"
        );
    }

    // 수면 시간 범위 오류
    public static SleepDataValidationException invalidSleepDuration(int hours) {
        return new SleepDataValidationException(
            "수면 시간이 비정상적입니다: " + hours + "시간 (0-24시간 범위)", 
            "INVALID_SLEEP_DURATION"
        );
    }

    // 미래 날짜 수면 기록
    public static SleepDataValidationException futureSleepDate() {
        return new SleepDataValidationException(
            "미래 날짜의 수면 기록은 등록할 수 없습니다", 
            "FUTURE_SLEEP_DATE"
        );
    }

    // 중복 수면 기록
    public static SleepDataValidationException duplicateSleepRecord() {
        return new SleepDataValidationException(
            "해당 날짜의 수면 기록이 이미 존재합니다", 
            HttpStatus.CONFLICT,
            "DUPLICATE_SLEEP_RECORD"
        );
    }

    // 수면 단계 데이터 불일치
    public static SleepDataValidationException sleepStageDataInconsistent() {
        return new SleepDataValidationException(
            "수면 단계별 시간의 합이 총 수면 시간과 일치하지 않습니다", 
            "SLEEP_STAGE_DATA_INCONSISTENT"
        );
    }

    // 심박수 데이터 비정상
    public static SleepDataValidationException invalidHeartRateData(int heartRate) {
        return new SleepDataValidationException(
            "비정상적인 심박수 데이터입니다: " + heartRate + "bpm", 
            "INVALID_HEART_RATE_DATA"
        );
    }

    // 수면 품질 점수 범위 오류
    public static SleepDataValidationException invalidSleepQualityScore(double score) {
        return new SleepDataValidationException(
            "수면 품질 점수가 유효 범위를 벗어났습니다: " + score + " (0.0-10.0)", 
            "INVALID_SLEEP_QUALITY_SCORE"
        );
    }

    // 플랫폼 데이터 동기화 충돌
    public static SleepDataValidationException platformDataConflict(String platform) {
        return new SleepDataValidationException(
            platform + " 플랫폼과 데이터 동기화 중 충돌이 발생했습니다", 
            HttpStatus.CONFLICT,
            "PLATFORM_DATA_CONFLICT"
        );
    }

    // 필수 수면 데이터 누락
    public static SleepDataValidationException missingSleepData(String missingField) {
        return new SleepDataValidationException(
            "필수 수면 데이터가 누락되었습니다: " + missingField, 
            "MISSING_SLEEP_DATA"
        );
    }
}