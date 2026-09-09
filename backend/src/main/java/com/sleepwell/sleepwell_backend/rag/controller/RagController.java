package com.sleepwell.sleepwell_backend.rag.controller;

import com.sleepwell.sleepwell_backend.exception.RagException;
import com.sleepwell.sleepwell_backend.rag.dto.RagQueryRequest;
import com.sleepwell.sleepwell_backend.rag.service.RagIndexService;
import com.sleepwell.sleepwell_backend.rag.service.RagQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RAG (Retrieval-Augmented Generation) API 컨트롤러
 * 수면 의학 전문 문서 기반 AI 질의응답 서비스를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rag")
@Tag(name = "RAG 질의응답", description = "수면 의학 전문 문서 기반 AI 질의응답 API")
public class RagController {

    private final RagIndexService indexService;
    private final RagQueryService queryService;

    public RagController(RagIndexService indexService, RagQueryService queryService) {
        this.indexService = indexService;
        this.queryService = queryService;
    }

    /**
     * RAG 질의응답 API
     *
     * 수면 의학 관련 질문에 대해 전문 의학 논문(PDF)을 검색하여
     * 근거 기반 답변을 생성합니다.
     *
     * @param req 질문 내용 및 검색 옵션
     * @return 답변, 출처 문서, 인용구 포함
     */
    @PostMapping("/query")
    @Operation(
        summary = "RAG 질의응답",
        description = """
            수면 의학 전문 문서(PDF)를 검색하여 질문에 답변합니다.

            ## 기능
            - 관리자가 색인한 수면 문서 청크 검색
            - 하이브리드 검색 (BM25 + 벡터)
            - 근거 기반 답변 생성
            - 출처 문서 및 인용구 제공

            ## 응답 형식
            - `message`: 전체 답변 (간단 + 상세)
            - `message_compact`: 간단 요약 답변
            - `message_detailed`: 상세 답변 (근거 포함)
            - `citations`: 출처 문서 배열 (제목, 인용구)

            ## 예시 질문
            - "갱년기 여성의 불면증 치료에 침술이 효과적인가요?"
            - "수면 무호흡증의 주요 증상은 무엇인가요?"
            - "근육 이완 운동이 수면의 질을 개선하나요?"
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "질의 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "갱년기 불면증 답변 예시",
                    value = """
                        {
                          "message": "### 간단보기\\n침술은 갱년기 여성의 불면증 개선에 도움이 될 수 있습니다...\\n\\n### 자세히보기\\n## 요약\\n- 침술이 갱년기 여성의 불면증 개선에 효과적일 수 있습니다.\\n\\n## 핵심 근거\\n- 침술 그룹에서 ISI가 11.35점 감소...",
                          "message_compact": "침술은 갱년기 여성의 불면증 개선에 도움이 될 수 있습니다. 연구 결과에 따르면...",
                          "message_detailed": "## 요약\\n- 침술이 갱년기 여성의 불면증 개선에 효과적일 수 있습니다.\\n\\n## 핵심 근거\\n- 침술 그룹에서 ISI가 평균 11.35점 감소...",
                          "citations": [
                            {
                              "title": "AcupunctureImprovesPeri-menopausalInsomnia.pdf",
                              "url": "",
                              "quote": "ISI score was 11.35 points in acupuncture group and 2.87 points in placebo-acupuncture group."
                            }
                          ]
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 필요합니다"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "질문 내용",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = RagQueryRequest.class),
            examples = {
                @ExampleObject(
                    name = "갱년기 불면증",
                    value = "{\"query\":\"갱년기 여성의 불면증 치료에 침술이 효과적인가요?\"}"
                ),
                @ExampleObject(
                    name = "수면 무호흡증",
                    value = "{\"query\":\"수면 무호흡증의 주요 증상은 무엇인가요?\"}"
                ),
                @ExampleObject(
                    name = "근육 이완 운동",
                    value = "{\"query\":\"점진적 근육 이완 운동이 수면의 질 개선에 효과적인가요?\"}"
                )
            }
        )
    )
    public ResponseEntity<Map<String, Object>> query(@RequestBody RagQueryRequest req) {
        try {
            log.info("RAG query received: length={}", req == null || req.query() == null ? 0 : req.query().length());
            Map<String, Object> answer = queryService.answer(req);
            log.info("RAG 질의 응답 완료");
            return ResponseEntity.ok(answer);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid RAG query: errorType={}", e.getClass().getSimpleName());
            throw RagException.invalidQuery("Invalid query or retrieval parameters");
        } catch (Exception e) {
            log.error("RAG query failed: errorType={}", e.getClass().getSimpleName());
            throw RagException.queryFailed("RAG query processing failed");
        }
    }
}
