package com.sleepwell.sleepwell_backend.dto.rag;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * RAG 파일 인덱싱 요청 DTO
 */
@Data
@Builder
public class RagFileIndexRequestDto {
    /**
     * 인덱싱할 파일 경로 목록
     */
    private List<String> filePaths;

    /**
     * 네임스페이스 (문서 그룹핑용)
     */
    private String namespace;

    /**
     * 네임스페이스 교체 여부
     * true일 경우 기존 네임스페이스의 문서를 모두 삭제하고 새로 인덱싱
     */
    private Boolean replaceNamespace;
}
