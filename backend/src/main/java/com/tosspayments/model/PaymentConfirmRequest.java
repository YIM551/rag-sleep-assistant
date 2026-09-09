package com.tosspayments.model;

/**
 * 토스페이먼츠 결제 승인 요청 더미 클래스
 */
public class PaymentConfirmRequest {
    private String paymentKey;
    private String orderId;
    private long amount;
    
    private PaymentConfirmRequest(Builder builder) {
        this.paymentKey = builder.paymentKey;
        this.orderId = builder.orderId;
        this.amount = builder.amount;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String paymentKey;
        private String orderId;
        private long amount;
        
        public Builder paymentKey(String paymentKey) {
            this.paymentKey = paymentKey;
            return this;
        }
        
        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }
        
        public Builder amount(long amount) {
            this.amount = amount;
            return this;
        }
        
        public PaymentConfirmRequest build() {
            return new PaymentConfirmRequest(this);
        }
    }
}