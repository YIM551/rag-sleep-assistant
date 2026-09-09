package com.iamport.request;

import java.math.BigDecimal;

/**
 * 아임포트 결제 취소 요청 더미 클래스
 * 실제 SDK가 없을 때 컴파일을 위한 임시 클래스
 */
public class CancelData {
    
    private String impUid;
    private String merchantUid;
    private BigDecimal amount;
    private BigDecimal taxFree;
    private BigDecimal vatAmount;
    private String reason;
    private String refundHolder;
    private String refundBank;
    private String refundAccount;
    private String refundTel;
    private boolean checksum;
    
    // Constructors
    public CancelData() {}
    
    public CancelData(String impUid, String reason) {
        this.impUid = impUid;
        this.reason = reason;
    }
    
    public CancelData(String impUid, BigDecimal amount, String reason) {
        this.impUid = impUid;
        this.amount = amount;
        this.reason = reason;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private CancelData cancelData = new CancelData();
        
        public Builder impUid(String impUid) {
            cancelData.impUid = impUid;
            return this;
        }
        
        public Builder merchantUid(String merchantUid) {
            cancelData.merchantUid = merchantUid;
            return this;
        }
        
        public Builder amount(BigDecimal amount) {
            cancelData.amount = amount;
            return this;
        }
        
        public Builder taxFree(BigDecimal taxFree) {
            cancelData.taxFree = taxFree;
            return this;
        }
        
        public Builder vatAmount(BigDecimal vatAmount) {
            cancelData.vatAmount = vatAmount;
            return this;
        }
        
        public Builder reason(String reason) {
            cancelData.reason = reason;
            return this;
        }
        
        public Builder refundHolder(String refundHolder) {
            cancelData.refundHolder = refundHolder;
            return this;
        }
        
        public Builder refundBank(String refundBank) {
            cancelData.refundBank = refundBank;
            return this;
        }
        
        public Builder refundAccount(String refundAccount) {
            cancelData.refundAccount = refundAccount;
            return this;
        }
        
        public Builder refundTel(String refundTel) {
            cancelData.refundTel = refundTel;
            return this;
        }
        
        public Builder checksum(boolean checksum) {
            cancelData.checksum = checksum;
            return this;
        }
        
        public CancelData build() {
            return cancelData;
        }
    }
    
    // Getters and Setters
    public String getImpUid() {
        return impUid;
    }
    
    public void setImpUid(String impUid) {
        this.impUid = impUid;
    }
    
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
    
    public BigDecimal getTaxFree() {
        return taxFree;
    }
    
    public void setTaxFree(BigDecimal taxFree) {
        this.taxFree = taxFree;
    }
    
    public BigDecimal getVatAmount() {
        return vatAmount;
    }
    
    public void setVatAmount(BigDecimal vatAmount) {
        this.vatAmount = vatAmount;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getRefundHolder() {
        return refundHolder;
    }
    
    public void setRefundHolder(String refundHolder) {
        this.refundHolder = refundHolder;
    }
    
    public String getRefundBank() {
        return refundBank;
    }
    
    public void setRefundBank(String refundBank) {
        this.refundBank = refundBank;
    }
    
    public String getRefundAccount() {
        return refundAccount;
    }
    
    public void setRefundAccount(String refundAccount) {
        this.refundAccount = refundAccount;
    }
    
    public String getRefundTel() {
        return refundTel;
    }
    
    public void setRefundTel(String refundTel) {
        this.refundTel = refundTel;
    }
    
    public boolean isChecksum() {
        return checksum;
    }
    
    public void setChecksum(boolean checksum) {
        this.checksum = checksum;
    }
}