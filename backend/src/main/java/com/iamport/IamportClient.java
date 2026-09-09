package com.iamport;

import com.iamport.response.IamportResponse;
import com.iamport.response.Payment;
import com.iamport.request.CancelData;
import com.iamport.request.PrepareData;

/**
 * 아임포트 클라이언트 더미 클래스
 * 실제 SDK가 없을 때 컴파일을 위한 임시 클래스
 */
public class IamportClient {
    
    private final String apiKey;
    private final String apiSecret;
    
    public IamportClient(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }
    
    public IamportResponse<Payment> paymentByImpUid(String impUid) throws Exception {
        // 더미 구현
        return new IamportResponse<>();
    }
    
    public IamportResponse<Payment> cancelPaymentByImpUid(CancelData cancelData) throws Exception {
        // 더미 구현
        return new IamportResponse<>();
    }
    
    public IamportResponse<Payment> prepare(PrepareData prepareData) throws Exception {
        // 더미 구현
        return new IamportResponse<>();
    }
    
    public IamportResponse<Payment> paymentByMerchantUid(String merchantUid) throws Exception {
        // 더미 구현
        return new IamportResponse<>();
    }
}