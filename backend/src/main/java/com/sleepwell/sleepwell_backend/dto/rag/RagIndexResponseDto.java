package com.sleepwell.sleepwell_backend.dto.rag;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 인덱싱 응답 DTO
 */
@Data
@Builder
public class RagIndexResponseDto {
    /**
     * 성공 여부
     */
    private Boolean success;

    /**
     * 메시지
     */
    private String message;

    /**
     * 인덱싱된 파일 수
     */
    private Integer indexedFileCount;

    /**
     * 인덱싱된 청크(문서 조각) 수
     */
    private Integer indexedChunkCount;

    /**
     * 실패한 파일 목록
     */
    private List<String> failedFiles;

    /**
     * 처리 시간 (초)
     */
    private Double processingTimeSeconds;

    /**
     * 완료 시간
     */
    private LocalDateTime completedAt;

    /**
     * 성공 응답 생성 헬퍼 메서드
     */
    public static RagIndexResponseDto success(int fileCount, int chunkCount, double processingTime) {
        return RagIndexResponseDto.builder()
                .success(true)
                .message("인덱싱 성공")
                .indexedFileCount(fileCount)
                .indexedChunkCount(chunkCount)
                .failedFiles(List.of())
                .processingTimeSeconds(processingTime)
                .completedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 삭제 응답 생성 헬퍼 메서드
     */
    public static RagIndexResponseDto deleted(int deletedCount) {
        return RagIndexResponseDto.builder()
                .success(true)
                .message("삭제 성공")
                .indexedFileCount(0)
                .indexedChunkCount(deletedCount)
                .failedFiles(List.of())
                .processingTimeSeconds(0.0)
                .completedAt(LocalDateTime.now())
                .build();
    }
}
