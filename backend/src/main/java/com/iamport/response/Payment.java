package com.iamport.response;

import java.math.BigDecimal;

/**
 * 아임포트 결제 정보 더미 클래스
 * 실제 SDK가 없을 때 컴파일을 위한 임시 클래스
 */
public class Payment {
    
    private String impUid;
    private String merchantUid;
    private String payMethod;
    private String channel;
    private String pgProvider;
    private String embPgProvider;
    private String pgTid;
    private String pgId;
    private boolean escrow;
    private String applyNum;
    private String bankCode;
    private String bankName;
    private String cardCode;
    private String cardName;
    private Integer cardQuota;
    private String cardNumber;
    private String cardType;
    private String vbankCode;
    private String vbankName;
    private String vbankNum;
    private String vbankHolder;
    private Long vbankDate;
    private String vbankIssuedAt;
    private String name;
    private BigDecimal amount;
    private BigDecimal cancelAmount;
    private String currency;
    private String buyerName;
    private String buyerEmail;
    private String buyerTel;
    private String buyerAddr;
    private String buyerPostcode;
    private String customData;
    private String userAgent;
    private String status;
    private Long startedAt;
    private Long paidAt;
    private Long failedAt;
    private Long cancelledAt;
    private String failReason;
    private String cancelReason;
    private String receiptUrl;
    private String cashReceiptIssued;
    private String customerUid;
    private String customerUidUsage;
    
    // Constructors
    public Payment() {}
    
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
    
    public String getPayMethod() {
        return payMethod;
    }
    
    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
    }
    
    public String getChannel() {
        return channel;
    }
    
    public void setChannel(String channel) {
        this.channel = channel;
    }
    
    public String getPgProvider() {
        return pgProvider;
    }
    
    public void setPgProvider(String pgProvider) {
        this.pgProvider = pgProvider;
    }
    
    public String getEmbPgProvider() {
        return embPgProvider;
    }
    
    public void setEmbPgProvider(String embPgProvider) {
        this.embPgProvider = embPgProvider;
    }
    
    public String getPgTid() {
        return pgTid;
    }
    
    public void setPgTid(String pgTid) {
        this.pgTid = pgTid;
    }
    
    public String getPgId() {
        return pgId;
    }
    
    public void setPgId(String pgId) {
        this.pgId = pgId;
    }
    
    public boolean isEscrow() {
        return escrow;
    }
    
    public void setEscrow(boolean escrow) {
        this.escrow = escrow;
    }
    
    public String getApplyNum() {
        return applyNum;
    }
    
    public void setApplyNum(String applyNum) {
        this.applyNum = applyNum;
    }
    
    public String getBankCode() {
        return bankCode;
    }
    
    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }
    
    public String getBankName() {
        return bankName;
    }
    
    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
    
    public String getCardCode() {
        return cardCode;
    }
    
    public void setCardCode(String cardCode) {
        this.cardCode = cardCode;
    }
    
    public String getCardName() {
        return cardName;
    }
    
    public void setCardName(String cardName) {
        this.cardName = cardName;
    }
    
    public Integer getCardQuota() {
        return cardQuota;
    }
    
    public void setCardQuota(Integer cardQuota) {
        this.cardQuota = cardQuota;
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    public String getCardType() {
        return cardType;
    }
    
    public void setCardType(String cardType) {
        this.cardType = cardType;
    }
    
    public String getVbankCode() {
        return vbankCode;
    }
    
    public void setVbankCode(String vbankCode) {
        this.vbankCode = vbankCode;
    }
    
    public String getVbankName() {
        return vbankName;
    }
    
    public void setVbankName(String vbankName) {
        this.vbankName = vbankName;
    }
    
    public String getVbankNum() {
        return vbankNum;
    }
    
    public void setVbankNum(String vbankNum) {
        this.vbankNum = vbankNum;
    }
    
    public String getVbankHolder() {
        return vbankHolder;
    }
    
    public void setVbankHolder(String vbankHolder) {
        this.vbankHolder = vbankHolder;
    }
    
    public Long getVbankDate() {
        return vbankDate;
    }
    
    public void setVbankDate(Long vbankDate) {
        this.vbankDate = vbankDate;
    }
    
    public String getVbankIssuedAt() {
        return vbankIssuedAt;
    }
    
    public void setVbankIssuedAt(String vbankIssuedAt) {
        this.vbankIssuedAt = vbankIssuedAt;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public BigDecimal getCancelAmount() {
        return cancelAmount;
    }
    
    public void setCancelAmount(BigDecimal cancelAmount) {
        this.cancelAmount = cancelAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getBuyerName() {
        return buyerName;
    }
    
    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }
    
    public String getBuyerEmail() {
        return buyerEmail;
    }
    
    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }
    
    public String getBuyerTel() {
        return buyerTel;
    }
    
    public void setBuyerTel(String buyerTel) {
        this.buyerTel = buyerTel;
    }
    
    public String getBuyerAddr() {
        return buyerAddr;
    }
    
    public void setBuyerAddr(String buyerAddr) {
        this.buyerAddr = buyerAddr;
    }
    
    public String getBuyerPostcode() {
        return buyerPostcode;
    }
    
    public void setBuyerPostcode(String buyerPostcode) {
        this.buyerPostcode = buyerPostcode;
    }
    
    public String getCustomData() {
        return customData;
    }
    
    public void setCustomData(String customData) {
        this.customData = customData;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
    }
    
    public Long getPaidAt() {
        return paidAt;
    }
    
    public void setPaidAt(Long paidAt) {
        this.paidAt = paidAt;
    }
    
    public Long getFailedAt() {
        return failedAt;
    }
    
    public void setFailedAt(Long failedAt) {
        this.failedAt = failedAt;
    }
    
    public Long getCancelledAt() {
        return cancelledAt;
    }
    
    public void setCancelledAt(Long cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
    
    public String getFailReason() {
        return failReason;
    }
    
    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }
    
    public String getCancelReason() {
        return cancelReason;
    }
    
    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
    
    public String getReceiptUrl() {
        return receiptUrl;
    }
    
    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }
    
    public String getCashReceiptIssued() {
        return cashReceiptIssued;
    }
    
    public void setCashReceiptIssued(String cashReceiptIssued) {
        this.cashReceiptIssued = cashReceiptIssued;
    }
    
    public String getCustomerUid() {
        return customerUid;
    }
    
    public void setCustomerUid(String customerUid) {
        this.customerUid = customerUid;
    }
    
    public String getCustomerUidUsage() {
        return customerUidUsage;
    }
    
    public void setCustomerUidUsage(String customerUidUsage) {
        this.customerUidUsage = customerUidUsage;
    }
}