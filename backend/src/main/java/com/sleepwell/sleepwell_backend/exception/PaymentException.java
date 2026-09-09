package com.sleepwell.sleepwell_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * 결제 관련 예외 클래스
 * 
 * 토스페이먼츠, 아임포트 등 결제 게이트웨이 연동 시 발생하는 예외를 처리합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
public class PaymentException extends BusinessException {

    public PaymentException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "PAYMENT_ERROR");
    }

    public PaymentException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }

    public PaymentException(String message, HttpStatus status, String errorCode) {
        super(message, status, errorCode);
    }

    // 결제 승인 실패
    public static PaymentException paymentApprovalFailed(String reason) {
        return new PaymentException("결제 승인에 실패했습니다: " + reason, "PAYMENT_APPROVAL_FAILED");
    }

    // 결제 취소 실패
    public static PaymentException paymentCancelFailed(String reason) {
        return new PaymentException("결제 취소에 실패했습니다: " + reason, "PAYMENT_CANCEL_FAILED");
    }

    // 결제 정보 검증 실패
    public static PaymentException paymentVerificationFailed() {
        return new PaymentException("결제 정보 검증에 실패했습니다", "PAYMENT_VERIFICATION_FAILED");
    }

    // 결제 게이트웨이 연결 실패
    public static PaymentException gatewayConnectionFailed(String gateway) {
        return new PaymentException(
            "결제 게이트웨이 연결에 실패했습니다: " + gateway, 
            HttpStatus.SERVICE_UNAVAILABLE, 
            "PAYMENT_GATEWAY_UNAVAILABLE"
        );
    }

    // 결제 한도 초과
    public static PaymentException paymentLimitExceeded() {
        return new PaymentException("결제 한도를 초과했습니다", "PAYMENT_LIMIT_EXCEEDED");
    }

    // 결제 수단 오류
    public static PaymentException invalidPaymentMethod() {
        return new PaymentException("지원하지 않는 결제 수단입니다", "INVALID_PAYMENT_METHOD");
    }

    // 중복 결제 시도
    public static PaymentException duplicatePayment() {
        return new PaymentException("이미 처리된 결제입니다", HttpStatus.CONFLICT, "DUPLICATE_PAYMENT");
    }
}