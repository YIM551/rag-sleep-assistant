package com.tosspayments;

import com.tosspayments.model.PaymentConfirmRequest;
import com.tosspayments.model.PaymentCancelRequest;
import com.tosspayments.model.PaymentObject;
import com.tosspayments.exception.TossPaymentsException;

/**
 * 토스페이먼츠 SDK 더미 클래스 (실제 SDK가 없을 때 컴파일용)
 */
public class TossPayments {
    
    private String secretKey;
    private int timeout;
    private int maxRetry;
    private boolean isProduction;
    
    private TossPayments(Builder builder) {
        this.secretKey = builder.secretKey;
        this.timeout = builder.timeout;
        this.maxRetry = builder.maxRetry;
        this.isProduction = builder.isProduction;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public PaymentObject confirm(PaymentConfirmRequest request) throws TossPaymentsException {
        // 더미 구현
        return new PaymentObject();
    }
    
    public PaymentObject confirmPayment(PaymentConfirmRequest request) throws TossPaymentsException {
        // 더미 구현 (테스트를 위한 별칭)
        return confirm(request);
    }
    
    public PaymentObject cancel(String paymentKey, PaymentCancelRequest request) throws TossPaymentsException {
        // 더미 구현
        return new PaymentObject();
    }
    
    public PaymentObject cancelPayment(String paymentKey, PaymentCancelRequest request) throws TossPaymentsException {
        // 더미 구현 (테스트를 위한 별칭)
        return cancel(paymentKey, request);
    }
    
    public PaymentObject getPayment(String paymentKey) throws TossPaymentsException {
        // 더미 구현
        return new PaymentObject();
    }
    
    public static class Builder {
        private String secretKey;
        private int timeout = 60000;
        private int maxRetry = 3;
        private boolean isProduction = false;
        
        public Builder secretKey(String secretKey) {
            this.secretKey = secretKey;
            return this;
        }
        
        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Builder maxRetry(int maxRetry) {
            this.maxRetry = maxRetry;
            return this;
        }
        
        public Builder production() {
            this.isProduction = true;
            return this;
        }
        
        public Builder test() {
            this.isProduction = false;
            return this;
        }
        
        public TossPayments build() {
            return new TossPayments(this);
        }
    }
}