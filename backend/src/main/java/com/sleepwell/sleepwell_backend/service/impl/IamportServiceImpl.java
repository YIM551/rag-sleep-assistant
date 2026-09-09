package com.sleepwell.sleepwell_backend.service.impl;

import com.iamport.IamportClient;
import com.iamport.request.CancelData;
import com.iamport.request.PrepareData;
import com.iamport.response.IamportResponse;
import com.iamport.response.Payment;
import com.sleepwell.sleepwell_backend.config.IamportConfig;
import com.sleepwell.sleepwell_backend.dto.payment.*;
import com.sleepwell.sleepwell_backend.entity.Subscription;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.PaymentStatus;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.repository.PaymentRepository;
import com.sleepwell.sleepwell_backend.repository.SubscriptionRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * 아임포트 결제 서비스 구현체
 * 다중 PG사 지원을 위한 아임포트 연동
 */
@Service("iamportService")
@Transactional
public class IamportServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(IamportServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final IamportConfig config;
    private final IamportClient iamportClient;

    @Autowired
    public IamportServiceImpl(PaymentRepository paymentRepository,
                             UserRepository userRepository,
                             SubscriptionRepository subscriptionRepository,
                             IamportConfig config,
                             IamportClient iamportClient) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.config = config;
        this.iamportClient = iamportClient;
    }

    @Override
    public PaymentPrepareResponseDto preparePayment(PaymentPrepareRequestDto request) {
        log.info("아임포트 결제 준비 시작: userId={}, amount={}", request.getUserId(), request.getAmount());

        // 사용자 검증
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 구독 검증 (구독 결제인 경우)
        Subscription subscription = null;
        if (request.getSubscriptionId() != null) {
            subscription = subscriptionRepository.findById(request.getSubscriptionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
        }

        // 결제 금액 검증
        if (request.getAmount().compareTo(BigDecimal.valueOf(100)) < 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 중복 결제 확인
        if (checkDuplicatePayment(request.getUserId(), request.getAmount())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
        }

        // 주문 ID 생성
        String orderId = generateOrderId();

        try {
            // 아임포트 결제 준비
            PrepareData prepareData = PrepareData.builder()
                    .merchantUid(orderId)
                    .amount(request.getAmount())
                    .build();

            IamportResponse<Payment> prepareResponse = iamportClient.prepare(prepareData);
            
            if (!prepareResponse.isSuccess()) {
                log.error("아임포트 결제 준비 실패: {}", prepareResponse.getMessage());
                throw new BusinessException(ErrorCode.PAYMENT_PREPARE_FAILED);
            }

            // 결제 정보 저장
            com.sleepwell.sleepwell_backend.entity.Payment payment = 
                com.sleepwell.sleepwell_backend.entity.Payment.builder()
                    .orderId(orderId)
                    .userId(request.getUserId())
                    .subscriptionId(request.getSubscriptionId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .paymentMethod(request.getPaymentMethod())
                    .status(PaymentStatus.PENDING)
                    .pgProvider("iamport")
                    .description(subscription != null ? subscription.getPlanName() + " 구독" : "일반 결제")
                    .createdAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);

            // 응답 생성
            return PaymentPrepareResponseDto.builder()
                    .orderId(orderId)
                    .amount(request.getAmount())
                    .orderName(payment.getDescription())
                    .customer(PaymentPrepareResponseDto.Customer.builder()
                            .email(user.getEmail())
                            .name(user.getName())
                            .phoneNumber(user.getPhoneNumber())
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("아임포트 결제 준비 중 오류 발생", e);
            throw new BusinessException(ErrorCode.PAYMENT_PREPARE_FAILED);
        }
    }

    @Override
    public PaymentResponseDto completePayment(PaymentCompleteRequestDto request) {
        log.info("아임포트 결제 완료 처리 시작: orderId={}", request.getOrderId());

        // 결제 정보 조회
        com.sleepwell.sleepwell_backend.entity.Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 결제 상태 검증
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 금액 검증
        if (payment.getAmount().compareTo(request.getAmount()) != 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        try {
            // 아임포트에서 결제 정보 조회
            IamportResponse<Payment> paymentResponse = iamportClient.paymentByMerchantUid(request.getOrderId());
            
            if (!paymentResponse.isSuccess()) {
                log.error("아임포트 결제 조회 실패: {}", paymentResponse.getMessage());
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("결제 조회 실패");
                paymentRepository.save(payment);
                throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
            }

            Payment iamportPayment = paymentResponse.getResponse();
            
            // 결제 상태 확인
            if (!"paid".equals(iamportPayment.getStatus())) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("결제 상태 불일치");
                paymentRepository.save(payment);
                throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
            }

            // 금액 재검증
            if (iamportPayment.getAmount().compareTo(request.getAmount()) != 0) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("결제 금액 불일치");
                paymentRepository.save(payment);
                throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
            }

            // 결제 완료 처리
            payment.setTransactionId(iamportPayment.getImpUid());
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.info("아임포트 결제 완료: orderId={}, impUid={}", request.getOrderId(), iamportPayment.getImpUid());

            return PaymentResponseDto.builder()
                    .orderId(payment.getOrderId())
                    .transactionId(payment.getTransactionId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .status(payment.getStatus())
                    .paymentMethod(payment.getPaymentMethod())
                    .paidAt(payment.getPaidAt())
                    .build();

        } catch (Exception e) {
            log.error("아임포트 결제 완료 처리 중 오류 발생", e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("시스템 오류");
            paymentRepository.save(payment);
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
    }

    @Override
    public PaymentResponseDto cancelPayment(PaymentCancelRequestDto request) {
        log.info("아임포트 결제 취소 시작: orderId={}", request.getOrderId());

        // 결제 정보 조회
        com.sleepwell.sleepwell_backend.entity.Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 취소 가능 상태 검증
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_NOT_ALLOWED);
        }

        try {
            // 아임포트 결제 취소
            CancelData cancelData = CancelData.builder()
                    .impUid(payment.getTransactionId())
                    .reason(request.getCancelReason())
                    .build();

            IamportResponse<Payment> cancelResponse = iamportClient.cancelPaymentByImpUid(cancelData);
            
            if (!cancelResponse.isSuccess()) {
                log.error("아임포트 결제 취소 실패: {}", cancelResponse.getMessage());
                throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
            }

            // 결제 상태 업데이트
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setFailureReason(request.getCancelReason());
            payment.setCancelledAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.info("아임포트 결제 취소 완료: orderId={}", request.getOrderId());

            return PaymentResponseDto.builder()
                    .orderId(payment.getOrderId())
                    .transactionId(payment.getTransactionId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .status(payment.getStatus())
                    .paymentMethod(payment.getPaymentMethod())
                    .failureReason(payment.getFailureReason())
                    .cancelledAt(payment.getCancelledAt())
                    .build();

        } catch (Exception e) {
            log.error("아임포트 결제 취소 중 오류 발생", e);
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }
    }

    @Override
    public PaymentRefundResponseDto refundPayment(PaymentRefundRequestDto request) {
        log.info("아임포트 부분 환불 시작: orderId={}, amount={}", request.getOrderId(), request.getRefundAmount());

        // 결제 정보 조회
        com.sleepwell.sleepwell_backend.entity.Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 환불 가능 상태 검증
        if (payment.getStatus() != PaymentStatus.COMPLETED && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED);
        }
        
        // 환불 금액 검증
        if (request.getRefundAmount() == null || request.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 환불 가능 금액 계산
        BigDecimal refundedAmount = payment.getRefundedAmount() != null ? payment.getRefundedAmount() : BigDecimal.ZERO;
        BigDecimal refundableAmount = payment.getAmount().subtract(refundedAmount);

        if (request.getRefundAmount().compareTo(refundableAmount) > 0) {
            throw new BusinessException(ErrorCode.REFUND_AMOUNT_EXCEEDED);
        }

        try {
            // 아임포트 부분 환불
            CancelData cancelData = CancelData.builder()
                    .impUid(payment.getTransactionId())
                    .amount(request.getRefundAmount())
                    .reason(request.getRefundReason())
                    .checksum(true)
                    .build();

            IamportResponse<Payment> refundResponse = iamportClient.cancelPaymentByImpUid(cancelData);
            
            if (!refundResponse.isSuccess()) {
                log.error("아임포트 부분 환불 실패: {}", refundResponse.getMessage());
                throw new BusinessException(ErrorCode.PAYMENT_REFUND_FAILED);
            }

            // 환불 정보 업데이트
            BigDecimal newRefundedAmount = refundedAmount.add(request.getRefundAmount());
            payment.setRefundedAmount(newRefundedAmount);
            
            if (newRefundedAmount.compareTo(payment.getAmount()) == 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
            
            paymentRepository.save(payment);

            BigDecimal remainingAmount = payment.getAmount().subtract(newRefundedAmount);

            log.info("아임포트 부분 환불 완료: orderId={}, refundAmount={}", request.getOrderId(), request.getRefundAmount());

            return PaymentRefundResponseDto.builder()
                    .orderId(payment.getOrderId())
                    .refundAmount(request.getRefundAmount())
                    .totalRefundedAmount(newRefundedAmount)
                    .remainingAmount(remainingAmount)
                    .status(payment.getStatus())
                    .refundReason(request.getRefundReason())
                    .build();

        } catch (Exception e) {
            log.error("아임포트 부분 환불 중 오류 발생", e);
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_FAILED);
        }
    }

    @Override
    public PaymentStatusResponseDto getPaymentStatus(String orderId) {
        com.sleepwell.sleepwell_backend.entity.Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        return PaymentStatusResponseDto.builder()
                .orderId(payment.getOrderId())
                .transactionId(payment.getTransactionId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .paidAt(payment.getPaidAt())
                .cancelledAt(payment.getCancelledAt())
                .failureReason(payment.getFailureReason())
                .isSynchronized(true)
                .build();
    }

    @Override
    public boolean processWebhook(PaymentWebhookDto webhookDto) {
        log.info("아임포트 웹훅 처리: eventType={}", webhookDto.getEventType());

        try {
            Map<String, Object> data = webhookDto.getData();
            if (data == null) {
                log.warn("웹훅 데이터가 null입니다");
                return true;
            }
            
            String impUid = (String) data.get("imp_uid");
            String merchantUid = (String) data.get("merchant_uid");

            if (impUid == null || merchantUid == null) {
                log.warn("웹훅 데이터 누락: impUid={}, merchantUid={}", impUid, merchantUid);
                return true;
            }

            // 결제 정보 조회
            Optional<com.sleepwell.sleepwell_backend.entity.Payment> paymentOpt = 
                paymentRepository.findByOrderId(merchantUid);

            if (paymentOpt.isEmpty()) {
                log.warn("결제 정보를 찾을 수 없음: merchantUid={}", merchantUid);
                return true;
            }

            com.sleepwell.sleepwell_backend.entity.Payment payment = paymentOpt.get();

            // 이벤트 타입별 처리
            switch (webhookDto.getEventType().toLowerCase()) {
                case "payment.paid":
                    if (payment.getStatus() == PaymentStatus.PENDING) {
                        payment.setStatus(PaymentStatus.COMPLETED);
                        payment.setTransactionId(impUid);
                        payment.setPaidAt(LocalDateTime.now());
                        paymentRepository.save(payment);
                    }
                    break;
                case "payment.failed":
                    if (payment.getStatus() == PaymentStatus.PENDING) {
                        payment.setStatus(PaymentStatus.FAILED);
                        payment.setFailureReason((String) data.get("fail_reason"));
                        paymentRepository.save(payment);
                    }
                    break;
                case "payment.cancelled":
                    payment.setStatus(PaymentStatus.CANCELLED);
                    payment.setFailureReason((String) data.get("cancel_reason"));
                    payment.setCancelledAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                    break;
                default:
                    log.info("처리하지 않는 웹훅 이벤트: {}", webhookDto.getEventType());
                    break;
            }

            return true;

        } catch (Exception e) {
            log.error("아임포트 웹훅 처리 중 오류 발생", e);
            return false;
        }
    }

    @Override
    public boolean validatePayment(PaymentValidationRequestDto request) {
        Optional<com.sleepwell.sleepwell_backend.entity.Payment> paymentOpt = 
            paymentRepository.findByOrderId(request.getOrderId());

        if (paymentOpt.isEmpty()) {
            return false;
        }

        com.sleepwell.sleepwell_backend.entity.Payment payment = paymentOpt.get();
        return payment.getAmount().compareTo(request.getAmount()) == 0;
    }

    @Override
    public PaymentStatisticsDto getPaymentStatistics(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<com.sleepwell.sleepwell_backend.entity.Payment> payments = 
            paymentRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int completedCount = 0;
        int failedCount = 0;
        int cancelledCount = 0;

        for (com.sleepwell.sleepwell_backend.entity.Payment payment : payments) {
            totalAmount = totalAmount.add(payment.getAmount());
            
            switch (payment.getStatus()) {
                case COMPLETED:
                    completedCount++;
                    break;
                case FAILED:
                    failedCount++;
                    break;
                case CANCELLED:
                case REFUNDED:
                    cancelledCount++;
                    break;
            }
        }

        BigDecimal averageAmount = payments.isEmpty() ? BigDecimal.ZERO : 
            totalAmount.divide(BigDecimal.valueOf(payments.size()), 2, BigDecimal.ROUND_HALF_UP);

        return PaymentStatisticsDto.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .completedCount(completedCount)
                .failedCount(failedCount)
                .cancelledCount(cancelledCount)
                .averageAmount(averageAmount)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private boolean checkDuplicatePayment(Long userId, BigDecimal amount) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);
        List<com.sleepwell.sleepwell_backend.entity.Payment> recentPayments = 
            paymentRepository.findByUserIdAndAmountAndStatusAndCreatedAtAfter(
                userId, amount, PaymentStatus.PENDING, cutoffTime);
        return !recentPayments.isEmpty();
    }

    @Override
    public Page<PaymentResponseDto> getPaymentHistory(Long userId, Pageable pageable) {
        Page<com.sleepwell.sleepwell_backend.entity.Payment> paymentPage = 
            paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<PaymentResponseDto> paymentDtos = paymentPage.getContent().stream()
                .map(payment -> PaymentResponseDto.builder()
                        .orderId(payment.getOrderId())
                        .transactionId(payment.getTransactionId())
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .status(payment.getStatus())
                        .paymentMethod(payment.getPaymentMethod())
                        .paidAt(payment.getPaidAt())
                        .cancelledAt(payment.getCancelledAt())
                        .failureReason(payment.getFailureReason())
                        .build())
                .toList();

        return new PageImpl<>(paymentDtos, pageable, paymentPage.getTotalElements());
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByStatus(Long userId, PaymentStatus status) {
        List<com.sleepwell.sleepwell_backend.entity.Payment> payments = 
            paymentRepository.findByUserIdAndStatus(userId, status);

        return payments.stream()
                .map(payment -> PaymentResponseDto.builder()
                        .orderId(payment.getOrderId())
                        .transactionId(payment.getTransactionId())
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .status(payment.getStatus())
                        .paymentMethod(payment.getPaymentMethod())
                        .paidAt(payment.getPaidAt())
                        .cancelledAt(payment.getCancelledAt())
                        .failureReason(payment.getFailureReason())
                        .build())
                .toList();
    }

    @Override
    public void expireOldPayments() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(config.getTimeout());
        List<com.sleepwell.sleepwell_backend.entity.Payment> expiredPayments = 
            paymentRepository.findExpiredPendingPayments(expireTime);

        for (com.sleepwell.sleepwell_backend.entity.Payment payment : expiredPayments) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("결제 시간 만료");
            paymentRepository.save(payment);
            log.info("결제 만료 처리 완료: orderId={}", payment.getOrderId());
        }
    }

    private String generateOrderId() {
        return "ORDER-IMP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}