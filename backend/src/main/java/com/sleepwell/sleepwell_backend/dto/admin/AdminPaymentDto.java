package com.sleepwell.sleepwell_backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentDto {
    private Long paymentId;
    private Long userId;
    private String userEmail;
    private String userName;
    
    private String orderId;
    private Double amount;
    private String currency;
    private String paymentMethod;
    private String paymentStatus;
    
    private String productName;
    private String productType;
    
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    
    private String pgProvider;
    private String pgTransactionId;
    
    private String failureReason;
    private Integer retryCount;
}