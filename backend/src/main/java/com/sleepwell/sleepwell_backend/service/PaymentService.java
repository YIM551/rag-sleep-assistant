package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.payment.*;
import com.sleepwell.sleepwell_backend.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 서비스 인터페이스
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
public interface PaymentService {

    /**
     * 결제 준비 (주문번호 생성 및 결제 정보 저장)
     */
    PaymentPrepareResponseDto preparePayment(PaymentPrepareRequestDto request);

    /**
     * 결제 승인 처리
     */
    PaymentResponseDto completePayment(PaymentCompleteRequestDto request);

    /**
     * 결제 취소
     */
    PaymentResponseDto cancelPayment(PaymentCancelRequestDto request);

    /**
     * 부분 환불
     */
    PaymentRefundResponseDto refundPayment(PaymentRefundRequestDto request);

    /**
     * 결제 상태 조회
     */
    PaymentStatusResponseDto getPaymentStatus(String orderId);

    /**
     * 사용자 결제 내역 조회
     */
    Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable);

    /**
     * 특정 상태의 결제 내역 조회
     */
    List<PaymentResponseDto> getPaymentsByStatus(Long userId, PaymentStatus status);

    /**
     * 결제 통계 조회
     */
    PaymentStatisticsDto getPaymentStatistics(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 웹훅 처리 (PG사 알림)
     */
    boolean processWebhook(PaymentWebhookDto webhook);

    /**
     * 결제 검증
     */
    boolean validatePayment(PaymentValidationRequestDto request);

    /**
     * 만료된 대기 결제 정리
     */
    void expireOldPayments();
}