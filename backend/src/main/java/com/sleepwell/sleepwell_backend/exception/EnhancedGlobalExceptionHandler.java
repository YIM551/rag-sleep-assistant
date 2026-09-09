package com.sleepwell.sleepwell_backend.exception;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러 (Enhanced Global Exception Handler)
 * 
 * SleepWell API의 모든 예외를 중앙 집중식으로 처리하는 핵심 컴포넌트입니다.
 * Spring의 @RestControllerAdvice를 활용하여 일관된 오류 응답 형식을 제공하고,
 * 보안과 사용자 경험을 모두 고려한 예외 처리 전략을 구현합니다.
 * 
 * 📋 주요 기능:
 * - 계층화된 예외 처리 (비즈니스 → 검증 → 보안 → HTTP → 시스템)
 * - 상세한 로깅 및 모니터링 지원
 * - 보안 고려한 오류 메시지 필터링
 * - 클라이언트 친화적 오류 응답 생성
 * - IP 주소 추적을 통한 보안 로그
 * 
 * 🔒 보안 특징:
 * - 민감한 시스템 정보 노출 방지
 * - 인증/권한 실패 시 상세 로깅
 * - 클라이언트 IP 주소 추적
 * - 일관된 오류 응답 형식으로 정보 유출 방지
 * 
 * 📊 모니터링 지원:
 * - 구조화된 로그 메시지
 * - 예외 타입별 분류 및 통계
 * - 성능 영향 최소화
 * - 운영 팀을 위한 상세 컨텍스트 제공
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see ErrorResponse
 * @see BusinessException
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Hidden
public class EnhancedGlobalExceptionHandler {

    private final MeterRegistry meterRegistry;

    // ===== 1. 비즈니스 로직 예외 =====
    
    /**
     * 사용자 정의 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        // 메트릭 기록
        recordExceptionMetric("business_exception", ex.getClass().getSimpleName(), ex.getStatus().value());

        // 예외 유형에 따른 로그 레벨 조정
        if (ex instanceof PaymentException || ex instanceof PlatformIntegrationException) {
            log.error("중요 비즈니스 로직 오류: {} - {} (IP: {})",
                request.getRequestURI(), ex.getMessage(), getClientIpAddress(request));
        } else {
            log.warn("비즈니스 로직 오류: {} - {}", request.getRequestURI(), ex.getMessage());
        }

        // BusinessException의 details(Map<String, String>)를 ErrorResponse의 details(Map<String, Object>)로 변환
        Map<String, Object> detailsObject = null;
        if (ex.getDetails() != null) {
            detailsObject = new HashMap<>(ex.getDetails());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .details(detailsObject)
                .build();

        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }
    
    /**
     * 결제 관련 예외 처리 (추가 로깅)
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(
            PaymentException ex, HttpServletRequest request) {

        // 메트릭 기록 (결제 실패는 중요하므로 별도 추적)
        recordExceptionMetric("payment_error", ex.getClass().getSimpleName(), ex.getStatus().value());

        log.error("결제 오류 발생: {} - {} (IP: {})",
            request.getRequestURI(), ex.getMessage(), getClientIpAddress(request));

        // 결제 오류는 추가 정보 제공
        Map<String, Object> details = new HashMap<>();
        if (ex.getDetails() != null) {
            details.putAll(ex.getDetails());
        }
        details.put("supportContact", "고객센터: 1588-0000");
        details.put("timestamp", LocalDateTime.now().toString());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }
    
    /**
     * 음성 처리 관련 예외 처리
     */
    @ExceptionHandler(VoiceProcessingException.class)
    public ResponseEntity<ErrorResponse> handleVoiceProcessingException(
            VoiceProcessingException ex, HttpServletRequest request) {
        
        log.warn("음성 처리 오류: {} - {}", request.getRequestURI(), ex.getMessage());

        // 음성 처리 가이드 정보 제공
        Map<String, Object> details = new HashMap<>();
        if (ex.getDetails() != null) {
            details.putAll(ex.getDetails());
        }
        details.put("supportedFormats", "WAV, MP3, M4A");
        details.put("maxFileSize", "10MB");
        details.put("maxDuration", "10분");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }
    
    /**
     * 수면 데이터 검증 관련 예외 처리
     */
    @ExceptionHandler(SleepDataValidationException.class)
    public ResponseEntity<ErrorResponse> handleSleepDataValidationException(
            SleepDataValidationException ex, HttpServletRequest request) {
        
        log.warn("수면 데이터 검증 오류: {} - {}", request.getRequestURI(), ex.getMessage());

        // 수면 데이터 가이드 정보 제공
        Map<String, Object> details = new HashMap<>();
        if (ex.getDetails() != null) {
            details.putAll(ex.getDetails());
        }
        details.put("validSleepDuration", "1-24시간");
        details.put("validQualityScore", "0.0-10.0");
        details.put("validHeartRate", "40-200 bpm");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }
    
    /**
     * 플랫폼 통합 관련 예외 처리
     */
    @ExceptionHandler(PlatformIntegrationException.class)
    public ResponseEntity<ErrorResponse> handlePlatformIntegrationException(
            PlatformIntegrationException ex, HttpServletRequest request) {
        
        log.error("플랫폼 연동 오류: {} - {} (IP: {})", 
            request.getRequestURI(), ex.getMessage(), getClientIpAddress(request));

        // 플랫폼 연동 가이드 정보 제공
        Map<String, Object> details = new HashMap<>();
        if (ex.getDetails() != null) {
            details.putAll(ex.getDetails());
        }
        details.put("supportedPlatforms", "Samsung Health, Apple Health, Google Fit");
        details.put("troubleshootingGuide", "/help/platform-integration");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    /**
     * RAG (Retrieval-Augmented Generation) 관련 예외 처리
     */
    @ExceptionHandler(RagException.class)
    public ResponseEntity<ErrorResponse> handleRagException(
            RagException ex, HttpServletRequest request) {

        // RAG 오류는 상세 로깅 (AI 모델 관련 중요 오류)
        log.error("RAG 시스템 오류: {} - {} (IP: {})",
            request.getRequestURI(), ex.getMessage(), getClientIpAddress(request));

        // RAG 관련 가이드 정보 제공
        Map<String, Object> details = new HashMap<>();
        if (ex.getDetails() != null) {
            details.putAll(ex.getDetails());
        }
        details.put("supportedFormats", "자연어 질문");
        details.put("exampleQuery", "갱년기 여성의 불면증 치료에 침술이 효과적인가요?");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    // ===== 2. 검증 관련 예외 =====
    
    /**
     * Bean Validation 예외 처리 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, Object> details = new HashMap<>();

        // 모든 필드 에러를 순회하며 Map에 담는다
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            @SuppressWarnings("unchecked")
            List<String> fieldErrors = (List<String>) details.computeIfAbsent(fieldName, k -> new ArrayList<String>());
            fieldErrors.add(errorMessage);
        });

        // 글로벌 에러도 함께 처리
        ex.getBindingResult().getGlobalErrors().forEach(error -> {
            @SuppressWarnings("unchecked")
            List<String> globalErrors = (List<String>) details.computeIfAbsent("globalErrors", k -> new ArrayList<String>());
            globalErrors.add(error.getDefaultMessage());
        });

        log.warn("검증 실패: {} - {}", request.getRequestURI(), details);
        
        // 글로벌 에러가 있고 필드 에러가 없을 경우, 글로벌 에러 메시지를 메인 메시지로 사용
        String message = "입력 데이터 검증에 실패했습니다";
        if (!ex.getBindingResult().getGlobalErrors().isEmpty() && ex.getBindingResult().getFieldErrors().isEmpty()) {
            message = ex.getBindingResult().getGlobalErrors().get(0).getDefaultMessage();
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(message) // 동적으로 메시지 설정
                .path(request.getRequestURI())
                .details(details)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 경로 변수 검증 예외 처리 (@PathVariable 검증)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        Map<String, Object> errors = violations.stream()
                .collect(Collectors.toMap(
                    violation -> violation.getPropertyPath().toString(),
                    ConstraintViolation::getMessage,
                    (existing, replacement) -> existing
                ));

        log.warn("경로 변수 검증 실패: {} - {}", request.getRequestURI(), errors);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Constraint Violation")
                .message("경로 변수 또는 파라미터가 유효하지 않습니다.")
                .path(request.getRequestURI())
                .details(errors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 필수 요청 파라미터 누락 예외 처리
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        String message = String.format("필수 파라미터 '%s'가 누락되었습니다", ex.getParameterName());
        
        log.warn("필수 파라미터 누락: {} - {}", request.getRequestURI(), message);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Missing Parameter")
                .message(message)
                .path(request.getRequestURI())
                .errorCode("MISSING_PARAMETER")
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 필수 멀티파트 요청 부분 누락 예외 처리
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPartException(
            MissingServletRequestPartException ex, HttpServletRequest request) {
        
        String message = String.format("필수 파일 또는 파트 '%s'가 누락되었습니다", ex.getRequestPartName());
        
        log.warn("필수 멀티파트 누락: {} - {}", request.getRequestURI(), message);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Missing Request Part")
                .message(message)
                .path(request.getRequestURI())
                .errorCode("MISSING_REQUEST_PART")
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * HTTP 메시지 읽기 불가 예외 처리 (JSON 파싱 오류, enum 값 오류 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        String message = "요청 데이터 형식이 올바르지 않습니다";
        
        // 구체적인 오류 메시지 추출
        if (ex.getCause() != null) {
            String causeMessage = ex.getCause().getMessage();
            if (causeMessage != null) {
                if (causeMessage.contains("not one of the values accepted for Enum class")) {
                    message = "지원되지 않는 열거형 값입니다";
                } else if (causeMessage.contains("Cannot deserialize")) {
                    message = "데이터 타입이 올바르지 않습니다";
                } else if (causeMessage.contains("JSON parse error")) {
                    message = "JSON 형식이 올바르지 않습니다";
                }
            }
        }
        
        log.error("HTTP 메시지 읽기 실패: {} - Message: {}, Cause: {}", 
            request.getRequestURI(), ex.getMessage(), 
            ex.getCause() != null ? ex.getCause().getMessage() : "no cause");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(message)
                .path(request.getRequestURI())
                .errorCode("INVALID_REQUEST_FORMAT")
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }


    // ===== 3. 보안 관련 예외 =====

    /**
     * OAuth2 인증 실패 예외 처리
     * 소셜 로그인 (Google, Kakao, Naver) 실패 시 상세한 에러 정보 제공
     */
    @ExceptionHandler(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleOAuth2AuthenticationException(
            org.springframework.security.oauth2.core.OAuth2AuthenticationException ex,
            HttpServletRequest request) {

        log.error("OAuth2 인증 실패: {} - {} (IP: {})",
            request.getRequestURI(), ex.getMessage(), getClientIpAddress(request));

        Map<String, Object> details = new HashMap<>();
        details.put("provider", extractProviderFromPath(request.getRequestURI()));
        details.put("errorCode", ex.getError() != null ? ex.getError().getErrorCode() : "unknown");
        details.put("errorDescription", ex.getError() != null ? ex.getError().getDescription() : ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("OAuth2 Authentication Failed")
                .message("소셜 로그인에 실패했습니다. 다시 시도해주세요.")
                .path(request.getRequestURI())
                .errorCode("OAUTH2_AUTHENTICATION_FAILED")
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * 일반 인증 실패 예외 처리
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            Exception ex, HttpServletRequest request) {

        // 메트릭 기록 (인증 실패 모니터링)
        recordExceptionMetric("authentication_error", ex.getClass().getSimpleName(), HttpStatus.UNAUTHORIZED.value());

        log.warn("인증 실패: {} - {} (IP: {})",
            request.getRequestURI(), ex.getMessage(), getClientIpAddress(request));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Failed")
                .message("인증에 실패했습니다")
                .path(request.getRequestURI())
                .errorCode("AUTHENTICATION_FAILED")
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * 권한 부족 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        log.warn("접근 권한 없음: {} - {} (IP: {})", 
            request.getRequestURI(), ex.getMessage(), getClientIpAddress(request));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("접근 권한이 없습니다")
                .path(request.getRequestURI())
                .errorCode("ACCESS_DENIED")
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    // ===== 4. HTTP 관련 예외 =====
    
    /**
     * 지원하지 않는 HTTP 메서드
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        log.warn("지원하지 않는 HTTP 메서드: {} {} - 지원 메서드: {}", 
            ex.getMethod(), request.getRequestURI(), ex.getSupportedMethods());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("Method Not Allowed")
                .message(String.format("HTTP %s 메서드는 지원되지 않습니다", ex.getMethod()))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    /**
     * 지원하지 않는 미디어 타입
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        
        log.warn("지원하지 않는 미디어 타입: {} - 지원 타입: {}", 
            ex.getContentType(), ex.getSupportedMediaTypes());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .error("Unsupported Media Type")
                .message("지원하지 않는 미디어 타입입니다")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    // ===== 5. 데이터 관련 예외 =====
    
    /**
     * 타입 변환 실패 예외 처리 (요청 파라미터 타입 불일치)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        log.warn("타입 변환 실패: {} - {}", request.getRequestURI(), ex.getMessage());
        
        String message = "잘못된 날짜 형식입니다.";
        if (!ex.getParameter().getParameterType().equals(LocalDate.class)) {
            message = String.format("파라미터 '%s'의 값이 올바르지 않습니다", ex.getName());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Type Mismatch")
                .message(message)
                .path(request.getRequestURI())
                .build();
            
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 날짜/시간 형식 오류 예외 처리
     */
    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<ErrorResponse> handleDateTimeException(
            DateTimeException ex, HttpServletRequest request) {
        
        log.warn("날짜/시간 파라미터 변환 오류: {} - {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("날짜 또는 시간 형식이 올바르지 않습니다")
                .path(request.getRequestURI())
                .errorCode("INVALID_DATE_FORMAT")
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 데이터베이스 제약 조건 위반
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.error("데이터 무결성 위반: {} - {}", request.getRequestURI(), ex.getMessage());

        String message = "데이터 제약 조건을 위반했습니다";
        HttpStatus status = HttpStatus.CONFLICT;
        String errorCode = "DATA_INTEGRITY_VIOLATION";

        // 템플릿 관련 검증 실패인지 확인
        String exceptionMessage = ex.getMessage();
        if (exceptionMessage != null) {
            String lowerMessage = exceptionMessage.toLowerCase();

            // null 제약 조건 위반 (필수 필드 누락)
            if (lowerMessage.contains("null") || lowerMessage.contains("not-null") ||
                lowerMessage.contains("column cannot be null")) {
                message = "필수 입력 항목이 누락되었습니다";
                status = HttpStatus.BAD_REQUEST;
                errorCode = "MISSING_REQUIRED_FIELD";
            }
            // 길이 제한 위반
            else if (lowerMessage.contains("too long") || lowerMessage.contains("data too long") ||
                     lowerMessage.contains("string or binary data would be truncated")) {
                message = "입력 데이터가 허용된 길이를 초과했습니다";
                status = HttpStatus.BAD_REQUEST;
                errorCode = "INPUT_TOO_LONG";
            }
            // 유니크 제약 조건 위반 (중복)
            else if (lowerMessage.contains("duplicate") || lowerMessage.contains("unique")) {
                message = "이미 존재하는 데이터입니다";
                status = HttpStatus.CONFLICT;
                errorCode = "DUPLICATE_DATA";
            }
            // 외래키 제약 조건 위반
            else if (lowerMessage.contains("foreign key") || lowerMessage.contains("references")) {
                message = "참조된 데이터를 찾을 수 없습니다";
                status = HttpStatus.BAD_REQUEST;
                errorCode = "INVALID_REFERENCE";
            }
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .errorCode(errorCode)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    // ===== 6. 파일 업로드 관련 예외 =====
    
    /**
     * 파일 크기 초과
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        
        log.warn("파일 크기 초과: {} - 최대 크기: {}", 
            request.getRequestURI(), ex.getMaxUploadSize());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .error("File Too Large")
                .message("파일 크기가 너무 큽니다")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    // ===== 7. JWT 및 토큰 관련 예외 =====
    
    /**
     * JWT 토큰 관련 예외 처리
     */
    @ExceptionHandler({io.jsonwebtoken.JwtException.class, 
                      io.jsonwebtoken.ExpiredJwtException.class,
                      io.jsonwebtoken.SignatureException.class,
                      io.jsonwebtoken.MalformedJwtException.class})
    public ResponseEntity<ErrorResponse> handleJwtException(
            Exception ex, HttpServletRequest request) {
        
        log.warn("JWT 토큰 오류: {} - {} (IP: {})", 
            request.getRequestURI(), ex.getMessage(), getClientIpAddress(request));

        String message = "토큰이 유효하지 않습니다";
        String errorCode = "INVALID_TOKEN";
        
        if (ex instanceof io.jsonwebtoken.ExpiredJwtException) {
            message = "토큰이 만료되었습니다";
            errorCode = "TOKEN_EXPIRED";
        } else if (ex instanceof io.jsonwebtoken.SignatureException) {
            message = "토큰 서명이 유효하지 않습니다";
            errorCode = "INVALID_TOKEN_SIGNATURE";
        } else if (ex instanceof io.jsonwebtoken.MalformedJwtException) {
            message = "토큰 형식이 올바르지 않습니다";
            errorCode = "MALFORMED_TOKEN";
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(message)
                .path(request.getRequestURI())
                .errorCode(errorCode)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    // ===== 8. 외부 API 관련 예외 =====
    
    /**
     * WebClient (외부 API 호출) 예외 처리
     */
    @ExceptionHandler({org.springframework.web.reactive.function.client.WebClientException.class,
                      org.springframework.web.reactive.function.client.WebClientRequestException.class,
                      org.springframework.web.reactive.function.client.WebClientResponseException.class})
    public ResponseEntity<ErrorResponse> handleWebClientException(
            Exception ex, HttpServletRequest request) {
        
        log.error("외부 API 호출 실패: {} - {}", request.getRequestURI(), ex.getMessage());

        String message = "외부 서비스 연결에 실패했습니다";
        String errorCode = "EXTERNAL_API_ERROR";
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        
        if (ex instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
            org.springframework.web.reactive.function.client.WebClientResponseException webEx = 
                (org.springframework.web.reactive.function.client.WebClientResponseException) ex;
            
            if (webEx.getStatusCode().value() == 429) {
                message = "외부 서비스 요청 한도를 초과했습니다";
                errorCode = "EXTERNAL_API_RATE_LIMIT";
                status = HttpStatus.TOO_MANY_REQUESTS;
            } else if (webEx.getStatusCode().is4xxClientError()) {
                message = "외부 서비스 요청이 올바르지 않습니다";
                errorCode = "EXTERNAL_API_CLIENT_ERROR";
                status = HttpStatus.BAD_REQUEST;
            }
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .errorCode(errorCode)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }
    
    // ===== 8. Spring AI 관련 예외 =====
    
    /**
     * OpenAI API 클라이언트 오류 처리
     */
    @ExceptionHandler(org.springframework.ai.openai.api.common.OpenAiApiClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleOpenAiApiClientErrorException(
            org.springframework.ai.openai.api.common.OpenAiApiClientErrorException ex, HttpServletRequest request) {
        
        log.error("OpenAI API 오류: {} - {}", request.getRequestURI(), ex.getMessage());

        String message = "AI 서비스 호출 중 오류가 발생했습니다";
        String errorCode = "OPENAI_API_ERROR";
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        
        // 4xx 오류에 따른 세분화된 처리
        String exceptionMessage = ex.getMessage();
        if (exceptionMessage != null) {
            if (exceptionMessage.contains("401") || exceptionMessage.toLowerCase().contains("api key")) {
                message = "AI 서비스 인증에 실패했습니다";
                errorCode = "OPENAI_AUTH_ERROR";
                status = HttpStatus.UNAUTHORIZED;
            } else if (exceptionMessage.contains("429") || exceptionMessage.toLowerCase().contains("rate limit")) {
                message = "AI 서비스 요청 한도를 초과했습니다";
                errorCode = "OPENAI_RATE_LIMIT";
                status = HttpStatus.TOO_MANY_REQUESTS;
            } else if (exceptionMessage.toLowerCase().contains("quota")) {
                message = "AI 서비스 할당량을 초과했습니다";
                errorCode = "OPENAI_QUOTA_EXCEEDED";
                status = HttpStatus.PAYMENT_REQUIRED;
            } else if (exceptionMessage.contains("400")) {
                message = "AI 서비스 요청이 올바르지 않습니다";
                errorCode = "OPENAI_BAD_REQUEST";
                status = HttpStatus.BAD_REQUEST;
            }
        }

        Map<String, Object> details = new HashMap<>();
        details.put("aiService", "OpenAI");
        details.put("retryAfter", "30초");
        details.put("supportContact", "고객센터: 1588-0000");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .errorCode(errorCode)
                .details(details)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Spring AI 일시적 예외 처리 (재시도 가능)
     */
    @ExceptionHandler(org.springframework.ai.retry.TransientAiException.class)
    public ResponseEntity<ErrorResponse> handleTransientAiException(
            org.springframework.ai.retry.TransientAiException ex, HttpServletRequest request) {
        
        log.warn("일시적 AI 서비스 오류: {} - {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("retryable", true);
        details.put("recommendation", "잠시 후 다시 시도해주세요");
        details.put("retryAfter", "30초");
        details.put("aiService", "Spring AI");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Temporarily Unavailable")
                .message("AI 서비스가 일시적으로 사용할 수 없습니다")
                .path(request.getRequestURI())
                .errorCode("AI_SERVICE_TRANSIENT_ERROR")
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    /**
     * Spring AI 비일시적 예외 처리 (재시도 불가)
     */
    @ExceptionHandler(org.springframework.ai.retry.NonTransientAiException.class)
    public ResponseEntity<ErrorResponse> handleNonTransientAiException(
            org.springframework.ai.retry.NonTransientAiException ex, HttpServletRequest request) {
        
        log.error("비일시적 AI 서비스 오류: {} - {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("retryable", false);
        details.put("supportContact", "고객센터: 1588-0000");
        details.put("troubleshootingGuide", "/help/ai-service-issues");
        details.put("aiService", "Spring AI");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("AI Service Error")
                .message("AI 서비스 요청을 처리할 수 없습니다")
                .path(request.getRequestURI())
                .errorCode("AI_SERVICE_NON_TRANSIENT_ERROR")
                .details(details)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Spring AI 도구 실행 예외 처리
     */
    @ExceptionHandler(org.springframework.ai.tool.execution.ToolExecutionException.class)
    public ResponseEntity<ErrorResponse> handleToolExecutionException(
            org.springframework.ai.tool.execution.ToolExecutionException ex, HttpServletRequest request) {
        
        log.error("AI 도구 실행 오류: {} - {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("aiService", "AI Tool");
        details.put("recommendation", "도구 설정을 확인하거나 관리자에게 문의하세요");
        details.put("supportContact", "고객센터: 1588-0000");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Tool Execution Error")
                .message("AI 도구 실행 중 오류가 발생했습니다")
                .path(request.getRequestURI())
                .errorCode("AI_TOOL_EXECUTION_ERROR")
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Spring WebClient 예외 처리 (Spring AI에서 사용)
     */
    @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessException(
            org.springframework.web.client.ResourceAccessException ex, HttpServletRequest request) {
        
        log.error("AI 서비스 리소스 접근 오류: {} - {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("aiService", "Spring AI");
        details.put("retryable", true);
        details.put("retryAfter", "30초");
        details.put("recommendation", "네트워크 연결을 확인하고 잠시 후 다시 시도해주세요");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("AI 서비스에 연결할 수 없습니다")
                .path(request.getRequestURI())
                .errorCode("AI_SERVICE_CONNECTION_ERROR")
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    // ===== 9. 데이터베이스 연결 관련 예외 =====
    
    /**
     * 데이터베이스 접근 예외 처리
     */
    @ExceptionHandler({org.springframework.dao.DataAccessException.class,
                      org.springframework.dao.QueryTimeoutException.class,
                      org.springframework.dao.PessimisticLockingFailureException.class,
                      org.springframework.dao.OptimisticLockingFailureException.class,
                      org.springframework.transaction.TransactionSystemException.class})
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            Exception ex, HttpServletRequest request) {
        
        log.error("데이터베이스 접근 오류: {} - {}", request.getRequestURI(), ex.getMessage());

        String message = "데이터베이스 연결에 문제가 발생했습니다";
        String errorCode = "DATABASE_ERROR";
        
        if (ex instanceof org.springframework.dao.QueryTimeoutException) {
            message = "데이터베이스 응답 시간이 초과되었습니다";
            errorCode = "DATABASE_TIMEOUT";
        } else if (ex instanceof org.springframework.dao.PessimisticLockingFailureException) {
            message = "다른 사용자가 동일한 데이터를 수정 중입니다";
            errorCode = "DATABASE_LOCK_ERROR";
        } else if (ex instanceof org.springframework.dao.OptimisticLockingFailureException) {
            message = "데이터가 다른 사용자에 의해 변경되었습니다";
            errorCode = "DATABASE_CONCURRENT_MODIFICATION";
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Database Error")
                .message(message)
                .path(request.getRequestURI())
                .errorCode(errorCode)
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    // ===== 10. Rate Limiting 관련 예외 =====
    
    /**
     * Rate Limiting 예외 처리
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitException(
            RateLimitExceededException ex, HttpServletRequest request) {
        
        log.warn("API 호출 한도 초과: {} - {} (IP: {})", 
            request.getRequestURI(), ex.getMessage(), getClientIpAddress(request));

        // Rate Limit 상세 정보 제공
        Map<String, Object> details = new HashMap<>();
        if (ex.getDetails() != null) {
            details.putAll(ex.getDetails());
        }
        details.put("retryAfter", "60초");
        details.put("maxRequests", "분당 100회");
        details.put("recommendation", "요청 간격을 늘려주세요");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }
    
    /**
     * 네트워크 타임아웃 예외 처리
     */
    @ExceptionHandler({java.net.SocketTimeoutException.class,
                      java.net.ConnectException.class,
                      java.io.IOException.class})
    public ResponseEntity<ErrorResponse> handleNetworkException(
            Exception ex, HttpServletRequest request) {
        
        log.error("네트워크 오류: {} - {}", request.getRequestURI(), ex.getMessage());

        String message = "네트워크 연결에 문제가 발생했습니다";
        String errorCode = "NETWORK_ERROR";
        
        if (ex instanceof java.net.SocketTimeoutException) {
            message = "네트워크 응답 시간이 초과되었습니다";
            errorCode = "NETWORK_TIMEOUT";
        } else if (ex instanceof java.net.ConnectException) {
            message = "서버에 연결할 수 없습니다";
            errorCode = "CONNECTION_FAILED";
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Network Error")
                .message(message)
                .path(request.getRequestURI())
                .errorCode(errorCode)
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    // ===== 11. 일반적인 예외 =====
    
    /**
     * 잘못된 요청 파라미터 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        log.warn("잘못된 요청 파라미터: {} - {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 상태 충돌 예외 처리 (예: 중복된 리소스 생성 시도)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        
        log.warn("상태 충돌 예외: {} - {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode("RESOURCE_CONFLICT")
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * 일반적인 RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        log.error("런타임 예외: {} - {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Runtime Error")
                .message("처리 중 오류가 발생했습니다")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 모든 예외의 최종 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        // 메트릭 기록 (예상치 못한 에러는 중요하므로 추적)
        recordExceptionMetric("unexpected_error", ex.getClass().getSimpleName(), HttpStatus.INTERNAL_SERVER_ERROR.value());

        log.error("예상치 못한 오류: {} - {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("서버 내부 오류가 발생했습니다")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // ===== 유틸리티 메서드 =====

    /**
     * 예외 메트릭 기록
     *
     * @param category 예외 카테고리 (business_exception, validation_error, etc.)
     * @param exceptionType 예외 타입
     * @param httpStatus HTTP 상태 코드
     */
    private void recordExceptionMetric(String category, String exceptionType, int httpStatus) {
        try {
            Counter.builder("sleepwell.exceptions")
                .description("발생한 예외 수")
                .tag("category", category)
                .tag("type", exceptionType)
                .tag("status", String.valueOf(httpStatus))
                .register(meterRegistry)
                .increment();
        } catch (Exception e) {
            // 메트릭 기록 실패는 조용히 처리 (핵심 기능 방해하지 않음)
            log.debug("메트릭 기록 실패: {}", e.getMessage());
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
            "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0];
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * OAuth2 제공자 추출 (URL 경로에서)
     * 예: /oauth2/authorization/google → google
     */
    private String extractProviderFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }

        if (path.contains("/google")) return "google";
        if (path.contains("/kakao")) return "kakao";
        if (path.contains("/naver")) return "naver";

        return "unknown";
    }
} 