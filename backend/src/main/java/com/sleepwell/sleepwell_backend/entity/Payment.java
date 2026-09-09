package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.PaymentMethod;
import com.sleepwell.sleepwell_backend.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 엔티티
 * 
 * 사용자의 결제 정보를 관리하는 엔티티입니다.
 * 토스페이먼츠, 아임포트 등 다양한 PG사를 통한 결제를 지원합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_user", columnList = "user_id"),
    @Index(name = "idx_payment_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"user", "subscription"})
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 결제한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 사용자 ID (조회 성능을 위한 추가 필드)
     */
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    /**
     * 관련 구독 정보 (구독 결제인 경우)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    /**
     * 구독 ID (조회 성능을 위한 추가 필드)
     */
    @Column(name = "subscription_id", insertable = false, updatable = false)
    private Long subscriptionId;

    /**
     * PG사 거래번호
     */
    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    /**
     * 내부 주문번호
     */
    @Column(name = "order_id", unique = true, nullable = false)
    private String orderId;

    /**
     * 결제 금액
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * 통화 (KRW, USD 등)
     */
    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "KRW";

    /**
     * 결제 방법
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    /**
     * 결제 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * PG사 이름 (TOSSPAYMENTS, IAMPORT 등)
     */
    @Column(name = "pg_provider", nullable = false)
    private String pgProvider;

    /**
     * 결제 설명
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 결제 완료 시간
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * 결제 취소 시간
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * 결제 실패 사유
     */
    @Column(name = "failure_reason")
    private String failureReason;

    /**
     * PG사 응답 데이터 (JSON)
     */
    @Column(name = "pg_response", columnDefinition = "TEXT")
    private String pgResponse;

    /**
     * 메타데이터 (추가 정보 저장용)
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * 환불 금액 (부분 환불 시)
     */
    @Column(name = "refunded_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    /**
     * 결제 완료 처리
     */
    public void complete(String transactionId, LocalDateTime paidAt) {
        this.transactionId = transactionId;
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = paidAt;
    }

    /**
     * 결제 취소 처리
     */
    public void cancel(String reason, LocalDateTime cancelledAt) {
        this.status = PaymentStatus.CANCELLED;
        this.failureReason = reason;
        this.cancelledAt = cancelledAt;
    }

    /**
     * 결제 실패 처리
     */
    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    /**
     * 부분 환불 처리
     */
    public void partialRefund(BigDecimal refundAmount) {
        this.refundedAmount = this.refundedAmount.add(refundAmount);
        if (this.refundedAmount.compareTo(this.amount) >= 0) {
            this.status = PaymentStatus.FULLY_REFUNDED;
        } else {
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
        }
    }

    /**
     * 전액 환불 처리
     */
    public void fullRefund() {
        this.refundedAmount = this.amount;
        this.status = PaymentStatus.FULLY_REFUNDED;
    }

    /**
     * 환불 가능 여부 확인
     */
    public boolean isRefundable() {
        return status == PaymentStatus.COMPLETED || 
               status == PaymentStatus.PARTIALLY_REFUNDED;
    }

    /**
     * 남은 환불 가능 금액 계산
     */
    public BigDecimal getRefundableAmount() {
        if (!isRefundable()) {
            return BigDecimal.ZERO;
        }
        return amount.subtract(refundedAmount);
    }
}