package com.sleepwell.sleepwell_backend.dto.payment;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * PG사 웹훅 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWebhookDto {

    /**
     * 이벤트 타입 (paid, cancelled, failed 등)
     */
    private String eventType;

    /**
     * PG사 이름
     */
    private String pgProvider;

    /**
     * 주문번호
     */
    private String orderId;

    /**
     * 거래번호
     */
    private String transactionId;

    /**
     * 결제 금액
     */
    private BigDecimal amount;

    /**
     * 결제 상태
     */
    private String status;

    /**
     * 이벤트 발생 시간
     */
    private LocalDateTime eventTime;

    /**
     * 원본 웹훅 데이터
     */
    private Map<String, Object> rawData;

    /**
     * 서명 (보안 검증용)
     */
    private String signature;
    
    /**
     * 데이터 (토스페이먼츠 웹훅용)
     */
    private Map<String, Object> data;
}