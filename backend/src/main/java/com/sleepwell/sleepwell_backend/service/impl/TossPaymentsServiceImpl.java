package com.sleepwell.sleepwell_backend.service.impl;

import com.sleepwell.sleepwell_backend.config.TossPaymentsConfig;
import com.sleepwell.sleepwell_backend.dto.payment.*;
import com.sleepwell.sleepwell_backend.entity.Payment;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.PaymentStatus;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.repository.PaymentRepository;
import com.sleepwell.sleepwell_backend.repository.SubscriptionRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.service.PaymentService;
import com.tosspayments.TossPayments;
import com.tosspayments.exception.TossPaymentsException;
import com.tosspayments.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 토스페이먼츠 결제 서비스 구현체
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Service("tossPaymentsService")
@RequiredArgsConstructor
@Transactional
public class TossPaymentsServiceImpl implements PaymentService {

    private final TossPayments tossPayments;
    private final TossPaymentsConfig config;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    private static final String PG_PROVIDER = "TOSSPAYMENTS";

    @Override
    public PaymentPrepareResponseDto preparePayment(PaymentPrepareRequestDto request) {
        log.info("토스페이먼츠 결제 준비 시작 - userId: {}, amount: {}", 
            request.getUserId(), request.getAmount());

        // 사용자 확인
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // 구독 확인 (구독 ID가 있는 경우)
        if (request.getSubscriptionId() != null) {
            subscriptionRepository.findById(request.getSubscriptionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
        }
        
        // 금액 검증
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 중복 결제 확인
        if (checkDuplicatePayment(request.getUserId(), request.getAmount())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
        }

        // 주문번호 생성
        String orderId = generateOrderId();

        // Payment 엔티티 생성 및 저장
        Payment payment = Payment.builder()
                .user(user)
                .orderId(orderId)
                .amount(request.getAmount())
                .currency("KRW")
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .pgProvider(PG_PROVIDER)
                .description(request.getProductName())
                .metadata(request.getMetadata())
                .build();

        if (request.getSubscriptionId() != null) {
            // 구독 결제 처리 로직 (필요시 구현)
        }

        payment = paymentRepository.save(payment);

        // 응답 DTO 생성
        return PaymentPrepareResponseDto.builder()
                .orderId(orderId)
                .amount(request.getAmount())
                .orderName(payment.getDescription())
                .clientKey(config.getClientKey())
                .customerName(request.getBuyerName())
                .customerEmail(request.getBuyerEmail())
                .customerMobilePhone(request.getBuyerTel())
                .customer(PaymentPrepareResponseDto.Customer.builder()
                        .email(user.getEmail())
                        .name(user.getName())
                        .phoneNumber(user.getPhoneNumber())
                        .build())
                .successUrl(request.getSuccessUrl() != null ? 
                    request.getSuccessUrl() : config.getSuccessUrl())
                .failUrl(request.getFailUrl() != null ? 
                    request.getFailUrl() : config.getFailUrl())
                .windowType(config.getWindowType())
                .easyPay(config.isEasyPayEnabled() ? config.getEasyPay() : null)
                .build();
    }

    @Override
    public PaymentResponseDto completePayment(PaymentCompleteRequestDto request) {
        log.info("토스페이먼츠 결제 승인 시작 - orderId: {}, paymentKey: {}", 
            request.getOrderId(), request.getPaymentKey());

        // 기존 결제 정보 조회
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 결제 상태 검증
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        
        // 금액 검증
        if (!payment.getAmount().equals(request.getAmount())) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        try {
            // 토스페이먼츠 결제 승인 API 호출
            PaymentConfirmRequest confirmRequest = PaymentConfirmRequest.builder()
                    .paymentKey(request.getPaymentKey())
                    .orderId(request.getOrderId())
                    .amount(request.getAmount().longValue())
                    .build();

            PaymentObject paymentObject = tossPayments.confirmPayment(confirmRequest);

            // 결제 정보 업데이트
            payment.complete(
                paymentObject.getPaymentKey(),
                LocalDateTime.parse(paymentObject.getApprovedAt(), 
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            );
            
            // PG 응답 데이터 저장
            payment.setPgResponse(paymentObject.toString());
            
            // 카드 정보가 있는 경우 저장
            if (paymentObject.getCard() != null) {
                request.setCardNumber(paymentObject.getCard().getNumber());
                request.setCardCompanyCode(paymentObject.getCard().getCompany());
                request.setApprovalNumber(paymentObject.getCard().getApproveNo());
            }

            payment = paymentRepository.save(payment);

            log.info("토스페이먼츠 결제 승인 완료 - paymentKey: {}", paymentObject.getPaymentKey());

            return convertToResponseDto(payment);

        } catch (TossPaymentsException e) {
            log.error("토스페이먼츠 결제 승인 실패 - code: {}, message: {}", 
                e.getCode(), e.getMessage());
            
            // 결제 실패 처리
            payment.fail(e.getMessage());
            paymentRepository.save(payment);
            
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, e.getMessage());
        }
    }

    @Override
    public PaymentResponseDto cancelPayment(PaymentCancelRequestDto request) {
        log.info("토스페이먼츠 결제 취소 시작 - orderId: {}", request.getOrderId());

        // 기존 결제 정보 조회
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 취소 가능 상태 검증
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_NOT_ALLOWED);
        }

        try {
            // 토스페이먼츠 결제 취소 API 호출
            PaymentCancelRequest cancelRequest = PaymentCancelRequest.builder()
                    .cancelReason(request.getCancelReason())
                    .cancelAmount(payment.getAmount().longValue())
                    .refundReceiveAccount(request.getRefundBankCode() != null ? 
                        RefundReceiveAccount.builder()
                            .bankCode(request.getRefundBankCode())
                            .accountNumber(request.getRefundAccountNumber())
                            .holderName(request.getRefundAccountHolder())
                            .build() : null)
                    .build();

            PaymentObject cancelledPayment = tossPayments.cancelPayment(
                payment.getTransactionId(), 
                cancelRequest
            );

            // 결제 정보 업데이트
            payment.cancel(
                request.getCancelReason(), 
                LocalDateTime.now()
            );
            payment = paymentRepository.save(payment);

            log.info("토스페이먼츠 결제 취소 완료 - paymentKey: {}", cancelledPayment.getPaymentKey());

            return convertToResponseDto(payment);

        } catch (TossPaymentsException e) {
            log.error("토스페이먼츠 결제 취소 실패 - code: {}, message: {}", 
                e.getCode(), e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED, e.getMessage());
        }
    }

    @Override
    public PaymentRefundResponseDto refundPayment(PaymentRefundRequestDto request) {
        log.info("토스페이먼츠 부분 환불 시작 - orderId: {}, refundAmount: {}", 
            request.getOrderId(), request.getRefundAmount());

        // 기존 결제 정보 조회
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 환불 가능 여부 검증
        if (!payment.isRefundable()) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED);
        }

        // 환불 가능 금액 검증
        BigDecimal refundableAmount = payment.getRefundableAmount();
        if (request.getRefundAmount().compareTo(refundableAmount) > 0) {
            throw new BusinessException(ErrorCode.REFUND_AMOUNT_EXCEEDED);
        }

        try {
            // 토스페이먼츠 부분 환불 API 호출
            PaymentCancelRequest refundRequest = PaymentCancelRequest.builder()
                    .cancelReason(request.getRefundReason())
                    .cancelAmount(request.getRefundAmount().longValue())
                    .refundReceiveAccount(request.getRefundAccountInfo() != null ? 
                        RefundReceiveAccount.builder()
                            .bankCode(request.getRefundAccountInfo().getBankCode())
                            .accountNumber(request.getRefundAccountInfo().getAccountNumber())
                            .holderName(request.getRefundAccountInfo().getAccountHolder())
                            .build() : null)
                    .build();

            PaymentObject refundedPayment = tossPayments.cancelPayment(
                payment.getTransactionId(), 
                refundRequest
            );

            // 결제 정보 업데이트
            if (request.getRefundAmount().compareTo(payment.getAmount()) >= 0) {
                payment.fullRefund();
            } else {
                payment.partialRefund(request.getRefundAmount());
            }
            payment = paymentRepository.save(payment);

            log.info("토스페이먼츠 부분 환불 완료 - paymentKey: {}, refundAmount: {}", 
                refundedPayment.getPaymentKey(), request.getRefundAmount());

            return PaymentRefundResponseDto.builder()
                    .orderId(payment.getOrderId())
                    .refundAmount(request.getRefundAmount())
                    .remainingAmount(payment.getAmount().subtract(payment.getRefundedAmount()))
                    .status(payment.getStatus())
                    .refundedAt(LocalDateTime.now())
                    .refundReason(request.getRefundReason())
                    .build();

        } catch (TossPaymentsException e) {
            log.error("토스페이먼츠 부분 환불 실패 - code: {}, message: {}", 
                e.getCode(), e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_FAILED, e.getMessage());
        }
    }

    @Override
    public PaymentStatusResponseDto getPaymentStatus(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 토스페이먼츠와 동기화가 필요한 경우
        if (payment.getStatus() == PaymentStatus.PENDING && 
            payment.getTransactionId() != null) {
            syncPaymentStatus(payment);
        }

        return PaymentStatusResponseDto.builder()
                .orderId(payment.getOrderId())
                .transactionId(payment.getTransactionId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .refundedAmount(payment.getRefundedAmount())
                .paidAt(payment.getPaidAt())
                .cancelledAt(payment.getCancelledAt())
                .failureReason(payment.getFailureReason())
                .isSynchronized(true)
                .build();
    }

    @Override
    public Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable) {
        Page<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return payments.map(this::convertToResponseDto);
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByStatus(Long userId, PaymentStatus status) {
        List<Payment> payments = paymentRepository.findByUserIdAndStatus(userId, status);
        return payments.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentStatisticsDto getPaymentStatistics(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Payment> payments = paymentRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate);
        
        BigDecimal totalAmount = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refundedAmount = payments.stream()
                .map(Payment::getRefundedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completedCount = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .count();

        long failedCount = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.FAILED)
                .count();

        return PaymentStatisticsDto.builder()
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .totalAmount(totalAmount)
                .totalRefundedAmount(refundedAmount)
                .completedCount(completedCount)
                .failedCount(failedCount)
                .cancelledCount(payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.CANCELLED)
                    .count())
                .averageAmount(completedCount > 0 ? 
                    totalAmount.divide(new BigDecimal(completedCount), 2, BigDecimal.ROUND_HALF_UP) : 
                    BigDecimal.ZERO)
                .build();
    }

    @Override
    public boolean processWebhook(PaymentWebhookDto webhook) {
        log.info("토스페이먼츠 웹훅 처리 시작 - eventType: {}, paymentKey: {}", 
            webhook.getEventType(), webhook.getData().get("paymentKey"));

        String paymentKey = (String) webhook.getData().get("paymentKey");
        String orderId = (String) webhook.getData().get("orderId");

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElse(null);

        if (payment == null) {
            log.warn("웹훅 처리 실패 - 결제 정보를 찾을 수 없음: {}", orderId);
            return false;
        }

        switch (webhook.getEventType()) {
            case "PAYMENT_STATUS_CHANGED":
                handlePaymentStatusChanged(payment, webhook);
                break;
            case "PAYMENT_CANCELLED":
                handlePaymentCancelled(payment, webhook);
                break;
            default:
                log.info("처리하지 않는 웹훅 이벤트 타입: {}", webhook.getEventType());
        }
        
        return true;
    }

    @Override
    public boolean validatePayment(PaymentValidationRequestDto request) {
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElse(null);

        if (payment == null) {
            return false;
        }

        return payment.getAmount().compareTo(request.getAmount()) == 0 &&
               payment.getStatus() == PaymentStatus.PENDING;
    }

    @Override
    public void expireOldPayments() {
        LocalDateTime expirationTime = LocalDateTime.now()
                .minusMinutes(config.getPaymentTimeoutMinutes());
        
        List<Payment> expiredPayments = paymentRepository.findExpiredPendingPayments(expirationTime);
        
        for (Payment payment : expiredPayments) {
            payment.fail("결제 시간 초과");
            paymentRepository.save(payment);
            log.info("만료된 결제 처리 - orderId: {}", payment.getOrderId());
        }
    }

    // Private helper methods

    private String generateOrderId() {
        return "ORDER_" + System.currentTimeMillis() + "_" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean checkDuplicatePayment(Long userId, BigDecimal amount) {
        LocalDateTime recentTime = LocalDateTime.now().minusMinutes(1);
        List<Payment> recentPayments = paymentRepository.findByUserIdAndAmountAndStatusAndCreatedAtAfter(
                userId, amount, PaymentStatus.PENDING, recentTime);
        return !recentPayments.isEmpty();
    }

    private void syncPaymentStatus(Payment payment) {
        try {
            PaymentObject paymentObject = tossPayments.getPayment(payment.getTransactionId());
            
            String status = paymentObject.getStatus();
            switch (status) {
                case "DONE":
                    payment.complete(
                        paymentObject.getPaymentKey(),
                        LocalDateTime.parse(paymentObject.getApprovedAt(), 
                            DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    );
                    break;
                case "CANCELED":
                    payment.cancel("토스페이먼츠 동기화", LocalDateTime.now());
                    break;
                case "PARTIAL_CANCELED":
                    BigDecimal canceledAmount = new BigDecimal(paymentObject.getCancelAmount());
                    payment.partialRefund(canceledAmount);
                    break;
                case "ABORTED":
                case "EXPIRED":
                    payment.fail(status);
                    break;
            }
            
            paymentRepository.save(payment);
        } catch (TossPaymentsException e) {
            log.error("결제 상태 동기화 실패 - orderId: {}, error: {}", 
                payment.getOrderId(), e.getMessage());
        }
    }

    private void handlePaymentStatusChanged(Payment payment, PaymentWebhookDto webhook) {
        String status = (String) webhook.getData().get("status");
        
        switch (status) {
            case "DONE":
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    String approvedAt = (String) webhook.getData().get("approvedAt");
                    payment.complete(
                        (String) webhook.getData().get("paymentKey"),
                        LocalDateTime.parse(approvedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    );
                }
                break;
            case "CANCELED":
                if (payment.getStatus() == PaymentStatus.COMPLETED) {
                    payment.cancel("웹훅 처리", LocalDateTime.now());
                }
                break;
        }
        
        paymentRepository.save(payment);
    }

    private void handlePaymentCancelled(Payment payment, PaymentWebhookDto webhook) {
        String cancellationReason = (String) webhook.getData().get("cancellationReason");
        payment.cancel(cancellationReason, LocalDateTime.now());
        paymentRepository.save(payment);
    }

    private PaymentResponseDto convertToResponseDto(Payment payment) {
        return PaymentResponseDto.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .pgProvider(payment.getPgProvider())
                .productName(payment.getDescription())
                .paidAt(payment.getPaidAt())
                .cancelledAt(payment.getCancelledAt())
                .failureReason(payment.getFailureReason())
                .refundedAmount(payment.getRefundedAmount())
                .refundableAmount(payment.getRefundableAmount())
                .metadata(payment.getMetadata())
                .build();
    }
}