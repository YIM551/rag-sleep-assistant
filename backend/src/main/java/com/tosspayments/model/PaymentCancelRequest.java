package com.tosspayments.model;

/**
 * 토스페이먼츠 결제 취소 요청 더미 클래스
 */
public class PaymentCancelRequest {
    private String cancelReason;
    private long cancelAmount;
    private RefundReceiveAccount refundReceiveAccount;
    
    private PaymentCancelRequest(Builder builder) {
        this.cancelReason = builder.cancelReason;
        this.cancelAmount = builder.cancelAmount;
        this.refundReceiveAccount = builder.refundReceiveAccount;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String cancelReason;
        private long cancelAmount;
        private RefundReceiveAccount refundReceiveAccount;
        
        public Builder cancelReason(String cancelReason) {
            this.cancelReason = cancelReason;
            return this;
        }
        
        public Builder cancelAmount(long cancelAmount) {
            this.cancelAmount = cancelAmount;
            return this;
        }
        
        public Builder refundReceiveAccount(RefundReceiveAccount refundReceiveAccount) {
            this.refundReceiveAccount = refundReceiveAccount;
            return this;
        }
        
        public PaymentCancelRequest build() {
            return new PaymentCancelRequest(this);
        }
    }
}