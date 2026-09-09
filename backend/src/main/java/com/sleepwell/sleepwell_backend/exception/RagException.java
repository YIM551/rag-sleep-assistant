package com.sleepwell.sleepwell_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * RAG (Retrieval-Augmented Generation) 관련 예외
 *
 * RAG 시스템의 문서 검색, 인덱싱, 쿼리 처리 중 발생하는 예외를 처리합니다.
 *
 * 주요 사용 케이스:
 * - 문서 인덱싱 실패
 * - 쿼리 처리 오류
 * - AI 모델 응답 실패
 * - 벡터 저장소 접근 오류
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
public class RagException extends BusinessException {

    /**
     * 메시지와 HTTP 상태 코드를 지정하여 RAG 예외 생성
     */
    public RagException(String message, HttpStatus status) {
        super(message, status, "RAG_ERROR");
    }

    /**
     * 메시지, HTTP 상태 코드, 에러 코드를 지정하여 RAG 예외 생성
     */
    public RagException(String message, HttpStatus status, String errorCode) {
        super(message, status, errorCode);
    }

    /**
     * 쿼리 처리 실패 예외 생성
     */
    public static RagException queryFailed(String message) {
        return new RagException(
            "RAG 쿼리 처리에 실패했습니다: " + message,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "RAG_QUERY_FAILED"
        );
    }

    /**
     * 문서 인덱싱 실패 예외 생성
     */
    public static RagException indexingFailed(String message) {
        return new RagException(
            "문서 인덱싱에 실패했습니다: " + message,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "RAG_INDEXING_FAILED"
        );
    }

    /**
     * AI 모델 응답 실패 예외 생성
     */
    public static RagException aiResponseFailed(String message) {
        return new RagException(
            "AI 모델 응답 생성에 실패했습니다: " + message,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "RAG_AI_RESPONSE_FAILED"
        );
    }

    /**
     * 잘못된 쿼리 입력 예외 생성
     */
    public static RagException invalidQuery(String message) {
        return new RagException(
            "잘못된 쿼리 입력입니다: " + message,
            HttpStatus.BAD_REQUEST,
            "RAG_INVALID_QUERY"
        );
    }

    /**
     * 벡터 저장소 접근 오류 예외 생성
     */
    public static RagException vectorStoreError(String message) {
        return new RagException(
            "벡터 저장소 접근 중 오류가 발생했습니다: " + message,
            HttpStatus.SERVICE_UNAVAILABLE,
            "RAG_VECTOR_STORE_ERROR"
        );
    }
}
