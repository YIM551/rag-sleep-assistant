package com.iamport.response;

/**
 * 아임포트 응답 더미 클래스
 * 실제 SDK가 없을 때 컴파일을 위한 임시 클래스
 */
public class IamportResponse<T> {
    
    private int code;
    private String message;
    private T response;
    
    public IamportResponse() {
        this.code = 0;
        this.message = "success";
    }
    
    public IamportResponse(int code, String message, T response) {
        this.code = code;
        this.message = message;
        this.response = response;
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getResponse() {
        return response;
    }
    
    public void setResponse(T response) {
        this.response = response;
    }
    
    public boolean isSuccess() {
        return code == 0;
    }
}