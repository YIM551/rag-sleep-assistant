package com.sleepwell.sleepwell_backend.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 메서드입니다"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "엔티티를 찾을 수 없습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 에러가 발생했습니다"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "잘못된 타입입니다"),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C006", "접근이 거부되었습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C007", "인증이 필요합니다"),

    // 사용자 관련 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용중인 이메일입니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U003", "잘못된 비밀번호입니다"),
    USER_DISABLED(HttpStatus.FORBIDDEN, "U004", "비활성화된 사용자입니다"),

    // 인증 관련 에러
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "만료된 토큰입니다"),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A003", "토큰을 찾을 수 없습니다"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A004", "리프레시 토큰이 만료되었습니다"),

    // 결제 관련 에러
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "결제 정보를 찾을 수 없습니다"),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "P002", "잘못된 결제 상태입니다"),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "P003", "결제에 실패했습니다"),
    PAYMENT_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "P004", "취소할 수 없는 결제입니다"),
    PAYMENT_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "P005", "결제 취소에 실패했습니다"),
    PAYMENT_REFUND_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "P006", "환불할 수 없는 결제입니다"),
    PAYMENT_REFUND_FAILED(HttpStatus.BAD_REQUEST, "P007", "환불 처리에 실패했습니다"),
    REFUND_AMOUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "P008", "환불 가능 금액을 초과했습니다"),
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "P009", "중복된 결제 요청입니다"),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "P010", "잘못된 결제 금액입니다"),
    PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_REQUEST, "P011", "결제 확인에 실패했습니다"),
    PAYMENT_EXPIRED(HttpStatus.GONE, "P012", "결제 요청이 만료되었습니다"),
    INVALID_REFUND_AMOUNT(HttpStatus.BAD_REQUEST, "P013", "환불 금액이 잘못되었습니다"),
    PAYMENT_PREPARE_FAILED(HttpStatus.BAD_REQUEST, "P014", "결제 준비에 실패했습니다"),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "P015", "결제 금액이 일치하지 않습니다"),

    // 구독 관련 에러
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "구독 정보를 찾을 수 없습니다"),
    SUBSCRIPTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "S002", "이미 구독중입니다"),
    SUBSCRIPTION_EXPIRED(HttpStatus.BAD_REQUEST, "S003", "만료된 구독입니다"),
    SUBSCRIPTION_CANCELLED(HttpStatus.BAD_REQUEST, "S004", "취소된 구독입니다"),

    // 수면 기록 관련 에러
    SLEEP_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "SR001", "수면 기록을 찾을 수 없습니다"),
    DUPLICATE_SLEEP_RECORD(HttpStatus.CONFLICT, "SR002", "중복된 수면 기록입니다"),
    INVALID_SLEEP_TIME(HttpStatus.BAD_REQUEST, "SR003", "잘못된 수면 시간입니다"),

    // 수면 설정 관련 에러
    SLEEP_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "SS001", "수면 설정을 찾을 수 없습니다"),
    DUPLICATE_SLEEP_SETTING(HttpStatus.CONFLICT, "SS002", "이미 수면 설정이 존재합니다"),
    INVALID_SLEEP_SETTING(HttpStatus.BAD_REQUEST, "SS003", "잘못된 수면 설정입니다"),

    // 스마트 타이밍 관련 에러
    SMART_TIMING_DISABLED(HttpStatus.BAD_REQUEST, "ST001", "스마트 타이밍 기능이 비활성화되어 있습니다"),
    INSUFFICIENT_SLEEP_DATA_FOR_ANALYSIS(HttpStatus.BAD_REQUEST, "ST002", "스마트 타이밍 분석을 위한 충분한 수면 데이터가 없습니다"),
    INVALID_TIMING_CALCULATION(HttpStatus.INTERNAL_SERVER_ERROR, "ST003", "타이밍 계산 중 오류가 발생했습니다"),
    TIMING_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ST004", "타이밍 분석에 실패했습니다"),

    // 스케줄링 관련 에러
    SCHEDULE_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SC001", "스케줄 업데이트에 실패했습니다"),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SC002", "해당 스케줄을 찾을 수 없습니다"),

    // 분석 관련 에러
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "AN001", "분석 결과를 찾을 수 없습니다"),
    ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AN002", "분석 처리에 실패했습니다"),
    ANALYSIS_IN_PROGRESS(HttpStatus.CONFLICT, "AN003", "이미 분석이 진행중입니다"),

    // 알림 관련 에러
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "알림을 찾을 수 없습니다"),
    NOTIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "N002", "알림 전송에 실패했습니다"),

    // 외부 API 관련 에러
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "E001", "외부 API 호출에 실패했습니다"),
    OPENAI_API_ERROR(HttpStatus.BAD_GATEWAY, "E002", "OpenAI API 호출에 실패했습니다"),
    CLAUDE_API_ERROR(HttpStatus.BAD_GATEWAY, "E003", "Claude API 호출에 실패했습니다"),

    // AI 서비스 관련 에러
    AI_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI001", "AI 서비스 처리 중 오류가 발생했습니다"),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI002", "AI 서비스를 사용할 수 없습니다"),
    ALL_AI_SERVICES_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI003", "모든 AI 서비스를 사용할 수 없습니다"),
    AI_RESPONSE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI004", "AI 응답 생성에 실패했습니다"),
    INSUFFICIENT_SLEEP_DATA(HttpStatus.BAD_REQUEST, "AI005", "분석을 위한 수면 데이터가 부족합니다"),
    VERTEX_AI_ERROR(HttpStatus.BAD_GATEWAY, "AI006", "Vertex AI 호출에 실패했습니다"),

    // ASMR 관련 에러
    ASMR_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ASMR001", "ASMR 콘텐츠를 찾을 수 없습니다"),
    ASMR_CONTENT_INACTIVE(HttpStatus.GONE, "ASMR002", "비활성화된 ASMR 콘텐츠입니다"),
    ASMR_INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "ASMR003", "유효하지 않은 ASMR 카테고리입니다"),
    ASMR_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ASMR004", "ASMR 콘텐츠 생성에 실패했습니다"),
    ASMR_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ASMR005", "ASMR 콘텐츠 업데이트에 실패했습니다"),
    ASMR_INVALID_DURATION(HttpStatus.BAD_REQUEST, "ASMR006", "유효하지 않은 콘텐츠 길이입니다"),
    ASMR_INVALID_PLAY_PARAMETERS(HttpStatus.BAD_REQUEST, "ASMR007", "유효하지 않은 재생 매개변수입니다"),

    // ASMR 파일 관련 에러 (Phase 2)
    ASMR_FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ASMR008", "ASMR 파일 업로드에 실패했습니다"),
    ASMR_FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ASMR009", "ASMR 파일 삭제에 실패했습니다"),
    ASMR_FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "ASMR010", "파일 크기가 제한을 초과했습니다"),
    ASMR_UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "ASMR011", "지원하지 않는 파일 형식입니다"),
    ASMR_FILE_CORRUPTED(HttpStatus.BAD_REQUEST, "ASMR012", "손상된 파일입니다"),
    ASMR_S3_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ASMR013", "스토리지 서비스 연결에 실패했습니다"),
    ASMR_QUALITY_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "ASMR014", "요청한 품질의 오디오를 찾을 수 없습니다"),
    ASMR_STREAM_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "ASMR015", "스트리밍 서비스를 사용할 수 없습니다"),
    ASMR_INVALID_RANGE_HEADER(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "ASMR016", "유효하지 않은 Range 요청입니다"),
    ASMR_ENCODING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ASMR017", "오디오 인코딩에 실패했습니다"),

    // ASMR 타이머 관련 에러 (Phase 3)
    ASMR_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "ASMR018", "ASMR 재생 세션을 찾을 수 없습니다"),
    ASMR_SESSION_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "ASMR019", "활성 상태가 아닌 세션입니다"),
    ASMR_SESSION_NOT_PAUSED(HttpStatus.BAD_REQUEST, "ASMR020", "일시 정지된 세션이 아닙니다"),
    ASMR_TIMER_NOT_SET(HttpStatus.BAD_REQUEST, "ASMR021", "타이머가 설정되지 않았습니다"),
    ASMR_TIMER_ALREADY_EXPIRED(HttpStatus.BAD_REQUEST, "ASMR022", "이미 만료된 타이머입니다"),
    ASMR_TIMER_INVALID_DURATION(HttpStatus.BAD_REQUEST, "ASMR023", "유효하지 않은 타이머 시간입니다"),
    ASMR_SESSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "ASMR024", "이미 진행 중인 세션이 있습니다"),
    ASMR_FADEOUT_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "ASMR025", "페이드아웃을 지원하지 않는 콘텐츠입니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ASMR026", "접근이 거부되었습니다"),

    // RAG 관련 에러
    RAG_INDEXING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RAG001", "RAG 인덱싱에 실패했습니다"),
    RAG_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RAG002", "RAG 문서 삭제에 실패했습니다"),
    RAG_INDEX_NOT_READY(HttpStatus.SERVICE_UNAVAILABLE, "RAG003", "RAG 인덱스가 준비되지 않았습니다"),

    // Rate Limiting 에러
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "R001", "요청 한도를 초과했습니다"),

    // Survey 관련 에러 (Normalized)
    SURVEY_RESPONSE_NOT_FOUND(HttpStatus.NOT_FOUND, "SV001", "설문 응답을 찾을 수 없습니다"),
    DUPLICATE_SURVEY_RESPONSE(HttpStatus.CONFLICT, "SV002", "이미 응답한 설문입니다"),
    INVALID_SURVEY_SCORE_RANGE(HttpStatus.BAD_REQUEST, "SV003", "설문 점수가 유효 범위를 벗어났습니다"),
    ISI_RESPONSE_NOT_FOUND(HttpStatus.NOT_FOUND, "SV004", "ISI 설문 응답을 찾을 수 없습니다"),
    ESS_RESPONSE_NOT_FOUND(HttpStatus.NOT_FOUND, "SV005", "ESS 설문 응답을 찾을 수 없습니다"),
    PSQI_RESPONSE_NOT_FOUND(HttpStatus.NOT_FOUND, "SV006", "PSQI 설문 응답을 찾을 수 없습니다"),
    BDI_RESPONSE_NOT_FOUND(HttpStatus.NOT_FOUND, "SV007", "BDI-II 설문 응답을 찾을 수 없습니다"),
    SIS_RESPONSE_NOT_FOUND(HttpStatus.NOT_FOUND, "SV008", "SIS 설문 응답을 찾을 수 없습니다"),

    // Activity Tracking 관련 에러
    ACTIVITY_TRACKING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AT001", "활동 추적에 실패했습니다"),
    ACTIVITY_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "AT002", "활동 이벤트를 찾을 수 없습니다"),
    ACTIVITY_STATISTICS_NOT_FOUND(HttpStatus.NOT_FOUND, "AT003", "활동 통계를 찾을 수 없습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}