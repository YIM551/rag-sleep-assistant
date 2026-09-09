package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.PaymentMethod;
import com.sleepwell.sleepwell_backend.enums.SubscriptionPlan;
import com.sleepwell.sleepwell_backend.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 구독 엔티티
 * 사용자의 구독 정보와 결제 정보를 관리
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(indexes = {
    // 사용자별 활성 구독 조회 (권한 확인, 기능 접근 제어)
    @Index(name = "IDX_SUBSCRIPTION_USER_STATUS", columnList = "user_id, status"),
    // 자동 갱신 대상 구독 조회 (배치 처리)
    @Index(name = "IDX_SUBSCRIPTION_AUTO_RENEWAL", columnList = "autoRenewal, nextBillingDate, status"),
    // 만료 예정 구독 알림용 (알림 발송 배치)
    @Index(name = "IDX_SUBSCRIPTION_EXPIRY", columnList = "endDate, status"),
    // 결제 실패 처리 및 외부 시스템 연동
    @Index(name = "IDX_SUBSCRIPTION_PAYMENT", columnList = "externalPaymentId, status"),
    // 구독 플랜별 통계 및 수익 분석
    @Index(name = "IDX_SUBSCRIPTION_PLAN_ANALYSIS", columnList = "planType, status, startDate"),
    // 취소된 구독 분석 (취소 사유 분석)
    @Index(name = "IDX_SUBSCRIPTION_CANCELLED", columnList = "cancelledAt, cancellationReason")
})
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 구독 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_SUBSCRIPTION_USER"))
    private User user;

    /**
     * 구독 플랜 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPlan planType;

    /**
     * 구독 플랜 이름
     */
    @Column(length = 100)
    private String planName;

    /**
     * 구독 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    /**
     * 구독 시작일
     */
    @Column(nullable = false)
    private LocalDateTime startDate;

    /**
     * 구독 종료일
     */
    @Column(nullable = false)
    private LocalDateTime endDate;

    /**
     * 다음 결제일
     */
    private LocalDateTime nextBillingDate;

    /**
     * 구독 가격
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * 통화 코드 (KRW, USD 등)
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * 결제 방법
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    /**
     * 외부 결제 시스템 ID (아임포트, 토스페이 등)
     */
    @Column(length = 100)
    private String externalPaymentId;

    /**
     * 자동 갱신 여부
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean autoRenewal = true;

    /**
     * 할인 쿠폰 코드
     */
    @Column(length = 50)
    private String couponCode;

    /**
     * 할인 금액
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /**
     * 취소 일시
     */
    private LocalDateTime cancelledAt;

    /**
     * 취소 사유
     */
    @Column(length = 500)
    private String cancellationReason;
} 