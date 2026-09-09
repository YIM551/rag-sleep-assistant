package com.sleepwell.sleepwell_backend.rag.controller;

import com.sleepwell.sleepwell_backend.dto.rag.RagFileIndexRequestDto;
import com.sleepwell.sleepwell_backend.dto.rag.RagIndexResponseDto;
import com.sleepwell.sleepwell_backend.dto.rag.RagIndexStatusResponseDto;
import com.sleepwell.sleepwell_backend.rag.service.RagIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAG 관리자 컨트롤러
 * 인덱싱, 삭제, 상태 조회 등 관리 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/rag")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "RAG 관리", description = "RAG 인덱싱 및 관리 API (관리자 전용)")
public class RagAdminController {

    private final RagIndexService ragIndexService;

    /**
     * 파일 인덱싱
     */
    @PostMapping("/index")
    @Operation(
        summary = "문서 인덱싱",
        description = """
            PDF, MD 파일을 RAG 시스템에 인덱싱합니다.

            ## 파일 경로
            - 서버 내부 경로: `/app/rag-corpus/파일명.pdf`
            - 예: `/app/rag-corpus/AcupunctureImproves.pdf`

            ## 네임스페이스
            - 문서 그룹 구분용 (예: `sleep-medicine`)
            - 생략 시 `default` 사용

            ## 예시 요청
            ```json
            {
              "filePaths": [
                "/app/rag-corpus/파일1.pdf",
                "/app/rag-corpus/파일2.pdf"
              ],
              "namespace": "sleep-medicine"
            }
            ```
            """
    )
    public ResponseEntity<RagIndexResponseDto> indexFiles(@RequestBody RagFileIndexRequestDto request) {
        log.info("[RagAdminController] 파일 인덱싱 요청 - 파일 수: {}, 네임스페이스: {}",
                request.getFilePaths().size(), request.getNamespace());

        RagIndexResponseDto response = ragIndexService.indexFiles(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 인덱스 상태 조회
     */
    @GetMapping("/status")
    @Operation(summary = "인덱스 상태 조회", description = "RAG 인덱스 통계 및 상태를 조회합니다")
    public ResponseEntity<RagIndexStatusResponseDto> getIndexStatus() {
        log.info("[RagAdminController] 인덱스 상태 조회");

        RagIndexStatusResponseDto status = ragIndexService.getStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 파일 삭제
     */
    @DeleteMapping("/files")
    @Operation(summary = "인덱스 파일 삭제", description = "특정 파일의 인덱스를 삭제합니다")
    public ResponseEntity<RagIndexResponseDto> removeFiles(@RequestBody List<String> filenames) {
        log.info("[RagAdminController] 파일 삭제 요청 - 파일 수: {}", filenames.size());

        RagIndexResponseDto response = ragIndexService.removeFiles(filenames);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 인덱스 재구축
     */
    @PostMapping("/rebuild")
    @Operation(summary = "전체 인덱스 재구축", description = "모든 인덱스를 삭제하고 재구축합니다")
    public ResponseEntity<RagIndexResponseDto> rebuildAll() {
        log.warn("[RagAdminController] 전체 인덱스 재구축 요청");

        RagIndexResponseDto response = ragIndexService.rebuildAll();
        return ResponseEntity.ok(response);
    }

    /**
     * 네임스페이스 삭제
     */
    @DeleteMapping("/namespace/{namespace}")
    @Operation(summary = "네임스페이스 삭제", description = "특정 네임스페이스의 모든 문서를 삭제합니다")
    public ResponseEntity<RagIndexResponseDto> deleteNamespace(@PathVariable String namespace) {
        log.warn("[RagAdminController] 네임스페이스 삭제 요청 - namespace: {}", namespace);

        RagIndexResponseDto response = ragIndexService.deleteNamespace(namespace);
        return ResponseEntity.ok(response);
    }
}
