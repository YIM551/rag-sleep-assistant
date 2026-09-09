package com.tosspayments.exception;

/**
 * 토스페이먼츠 예외 더미 클래스
 */
public class TossPaymentsException extends Exception {
    private String code;
    
    public TossPaymentsException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
}