package com.tosspayments.model;

import java.util.List;
import java.util.Map;

/**
 * 토스페이먼츠 결제 객체 더미 클래스
 */
public class PaymentObject {
    private String paymentKey;
    private String orderId;
    private String status;
    private String approvedAt;
    private String canceledAt;
    private long cancelAmount;
    private long totalAmount;
    private String method;
    private Card card;
    private List<Map<String, Object>> cancels;
    
    public String getPaymentKey() {
        return paymentKey;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getApprovedAt() {
        return approvedAt;
    }
    
    public String getCanceledAt() {
        return canceledAt;
    }
    
    public long getCancelAmount() {
        return cancelAmount;
    }
    
    public long getTotalAmount() {
        return totalAmount;
    }
    
    public String getMethod() {
        return method;
    }
    
    public Card getCard() {
        return card;
    }
    
    public List<Map<String, Object>> getCancels() {
        return cancels;
    }
    
    public static class Card {
        private String number;
        private String company;
        private String approveNo;
        
        public String getNumber() {
            return number;
        }
        
        public String getCompany() {
            return company;
        }
        
        public String getApproveNo() {
            return approveNo;
        }
    }
}