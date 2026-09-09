package com.tosspayments.model;

/**
 * 토스페이먼츠 환불 계좌 정보 더미 클래스
 */
public class RefundReceiveAccount {
    private String bankCode;
    private String accountNumber;
    private String holderName;
    
    private RefundReceiveAccount(Builder builder) {
        this.bankCode = builder.bankCode;
        this.accountNumber = builder.accountNumber;
        this.holderName = builder.holderName;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String bankCode;
        private String accountNumber;
        private String holderName;
        
        public Builder bankCode(String bankCode) {
            this.bankCode = bankCode;
            return this;
        }
        
        public Builder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }
        
        public Builder holderName(String holderName) {
            this.holderName = holderName;
            return this;
        }
        
        public RefundReceiveAccount build() {
            return new RefundReceiveAccount(this);
        }
    }
}