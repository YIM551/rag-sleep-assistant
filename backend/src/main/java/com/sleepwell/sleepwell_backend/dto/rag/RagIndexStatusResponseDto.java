package com.sleepwell.sleepwell_backend.dto.rag;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * RAG 인덱스 상태 응답 DTO
 */
@Data
@Builder
public class RagIndexStatusResponseDto {
    /**
     * 인덱스 준비 상태
     */
    private Boolean ready;

    /**
     * 전체 문서 수
     */
    private Integer totalDocuments;

    /**
     * 전체 청크 수
     */
    private Integer totalChunks;

    /**
     * 네임스페이스별 문서 수
     */
    private Map<String, Integer> namespaceDocumentCounts;

    /**
     * 인덱스 크기 (MB)
     */
    private Double indexSizeMB;

    /**
     * 마지막 인덱싱 시간
     */
    private LocalDateTime lastIndexedAt;

    /**
     * Lucene 인덱스 경로
     */
    private String luceneIndexPath;

    /**
     * Qdrant 컬렉션 이름
     */
    private String qdrantCollection;

    /**
     * 벡터 차원 수
     */
    private Integer vectorDimension;
}
