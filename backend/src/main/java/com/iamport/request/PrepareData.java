package com.iamport.request;

import java.math.BigDecimal;

/**
 * 아임포트 결제 준비 요청 더미 클래스
 * 실제 SDK가 없을 때 컴파일을 위한 임시 클래스
 */
public class PrepareData {
    
    private String merchantUid;
    private BigDecimal amount;
    
    // Constructors
    public PrepareData() {}
    
    public PrepareData(String merchantUid, BigDecimal amount) {
        this.merchantUid = merchantUid;
        this.amount = amount;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private PrepareData prepareData = new PrepareData();
        
        public Builder merchantUid(String merchantUid) {
            prepareData.merchantUid = merchantUid;
            return this;
        }
        
        public Builder amount(BigDecimal amount) {
            prepareData.amount = amount;
            return this;
        }
        
        public PrepareData build() {
            return prepareData;
        }
    }
    
    // Getters and Setters
    public String getMerchantUid() {
        return merchantUid;
    }
    
    public void setMerchantUid(String merchantUid) {
        this.merchantUid = merchantUid;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}