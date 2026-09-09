package com.sleepwell.sleepwell_backend.rag.service;

import com.sleepwell.sleepwell_backend.dto.rag.RagFileIndexRequestDto;
import com.sleepwell.sleepwell_backend.dto.rag.RagIndexResponseDto;
import com.sleepwell.sleepwell_backend.dto.rag.RagIndexStatusResponseDto;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.rag.dto.RagIndexRequest;
import com.sleepwell.sleepwell_backend.rag.infra.EmbeddingClient;
import com.sleepwell.sleepwell_backend.rag.infra.SparseStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 인덱싱 서비스
 * - Sparse(Lucene) + Dense(Qdrant VectorStore) 통합 인덱싱
 *
 * ✅ 근본 해결 포인트
 * - Dense 인덱싱 metadata를 표준 스키마로 통일
 * (filename/title/source/filePath/namespace/chunkIndex/chunkId/docId/ingestId/ingestedAt)
 * - citations에서 "Reference" fallback이 뜨지 않도록 filename/title을 항상 채움
 *
 * 파일 위치:
 * sleepwell-backend/src/main/java/com/sleepwell/sleepwell_backend/rag/service/RagIndexService.java
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagIndexService {

  // 다른 서비스/DI 호환을 위해 유지 (이 파일에서는 직접 사용하지 않아도 됨)
  private final EmbeddingClient embedding;

  private final VectorStore vector;
  private final SparseStore sparseStore;
  private final Environment env;

  private static final int VECTOR_ADD_BATCH_SIZE = 64;

  // Qdrant REST는 기본 6333 (Spring AI gRPC 포트와 다를 수 있음)
  private static final String DEFAULT_QDRANT_REST_PORT = "6333";

  private static final int DEFAULT_CHUNK_SIZE = 1200;
  private static final int DEFAULT_CHUNK_OVERLAP = 200;

  /**
   * Dense metadata 표준 키
   */
  private static final class Meta {
    private Meta() {
    }

    // retrieval/citation용 핵심 키
    static final String NAMESPACE = "namespace";
    static final String FILENAME = "filename"; // ✅ RagQueryService에서 우선순위로 보는 키
    static final String TITLE = "title"; // ✅ filename이 없을 때 fallback (여기도 같이 채움)
    static final String SOURCE = "source"; // ✅ URI (file:///...) 권장
    static final String FILE_PATH = "filePath";

    // chunk 식별/관리
    static final String CHUNK_INDEX = "chunkIndex";
    static final String CHUNK_ID = "chunkId"; // payload 내부 식별자 (Qdrant point id와 혼동 방지)
    static final String DOC_ID = "docId"; // 파일 단위 stable id
    static final String INGEST_ID = "ingestId"; // 인덱싱 실행 단위 id
    static final String INGESTED_AT = "ingestedAt";

    // 부가
    static final String MIME_TYPE = "mimeType";
    static final String SOURCE_NAME = "sourceName"; // 사람 읽기용(파일명)
  }

  /**
   * 레거시 인덱싱 메서드 (VectorStore에 문서 추가)
   * - chunk 단위 Document를 VectorStore에 add (VectorStore가 embedding/upsert 수행)
   *
   * ※ 파일 기반이 아니므로 filename/title은 "legacy"로 둠 (그래도 표준 키는 맞춰줌)
   */
  public void index(RagIndexRequest req) {
    if (req == null || req.docs() == null || req.docs().isEmpty())
      return;

    int chunkSize = resolveChunkSize();
    int overlap = resolveChunkOverlap();

    String ingestId = UUID.randomUUID().toString();
    String ingestedAt = OffsetDateTime.now().toString();

    List<Document> batch = new ArrayList<>(VECTOR_ADD_BATCH_SIZE);

    for (var d : req.docs()) {
      String text = (d == null) ? null : d.text();
      if (text == null || text.isBlank())
        continue;

      List<String> chunks = simpleChunks(text, chunkSize, overlap);
      for (int i = 0; i < chunks.size(); i++) {
        Map<String, Object> meta = new HashMap<>();
        meta.put(Meta.NAMESPACE, "default");
        meta.put(Meta.FILENAME, "legacy");
        meta.put(Meta.TITLE, "legacy");
        meta.put(Meta.SOURCE, "legacy");
        meta.put(Meta.SOURCE_NAME, "legacy");
        meta.put(Meta.FILE_PATH, "");
        meta.put(Meta.CHUNK_INDEX, i);
        meta.put(Meta.CHUNK_ID, UUID.randomUUID().toString());
        meta.put(Meta.DOC_ID, stableDocId("default", "legacy"));
        meta.put(Meta.INGEST_ID, ingestId);
        meta.put(Meta.INGESTED_AT, ingestedAt);
        meta.put(Meta.MIME_TYPE, "text/plain");

        batch.add(new Document(chunks.get(i), meta));

        if (batch.size() >= VECTOR_ADD_BATCH_SIZE) {
          vector.add(batch);
          batch.clear();
        }
      }
    }

    if (!batch.isEmpty())
      vector.add(batch);
  }

  /**
   * 파일 기반 인덱싱 (Lucene + Qdrant 모두)
   */
  public RagIndexResponseDto indexFiles(RagFileIndexRequestDto request) {
    long startTime = System.currentTimeMillis();

    try {
      if (request == null)
        throw new BusinessException(ErrorCode.RAG_INDEXING_FAILED);

      String namespace = Optional.ofNullable(request.getNamespace()).orElse("default");
      List<Path> paths = resolvePathsFromRequest(request);

      if (paths.isEmpty()) {
        log.warn("[RagIndexService] 인덱싱 대상 filePaths가 비어있음. namespace={}", namespace);
        return RagIndexResponseDto.builder()
            .success(true)
            .message("인덱싱 성공 (대상 파일 0개: filePaths를 전달하세요)")
            .indexedFileCount(0)
            .indexedChunkCount(0)
            .failedFiles(List.of())
            .processingTimeSeconds((System.currentTimeMillis() - startTime) / 1000.0)
            .completedAt(java.time.LocalDateTime.now())
            .build();
      }

      // replaceNamespace: Lucene + Qdrant(best-effort) 둘 다 정리
      if (Boolean.TRUE.equals(request.getReplaceNamespace()) && request.getNamespace() != null) {
        log.info("[RagIndexService] 네임스페이스 교체 모드: {}", namespace);

        // 1) Lucene
        sparseStore.deleteByNamespace(namespace);

        // 2) Qdrant (payload.namespace 기반 삭제)
        try {
          deleteVectorsByNamespace(namespace);
        } catch (Exception ex) {
          log.warn("[RagIndexService] Qdrant namespace delete 실패(best-effort). namespace={}, msg={}",
              namespace, ex.getMessage());
        }
      }

      // 1) Lucene 인덱싱
      int sparseChunkCount = sparseStore.indexFiles(paths, namespace);

      // 2) Qdrant(Dense) 인덱싱
      DenseIndexResult dense = indexDenseToVectorStore(paths, namespace);

      double processingTime = (System.currentTimeMillis() - startTime) / 1000.0;

      return RagIndexResponseDto.builder()
          .success(true)
          .message(String.format("인덱싱 성공 (luceneChunks=%d, qdrantChunks=%d)", sparseChunkCount, dense.indexedChunks()))
          .indexedFileCount(paths.size())
          .indexedChunkCount(dense.indexedChunks())
          .failedFiles(dense.failedFiles())
          .processingTimeSeconds(processingTime)
          .completedAt(java.time.LocalDateTime.now())
          .build();

    } catch (BusinessException be) {
      throw be;
    } catch (Exception e) {
      log.error("[RagIndexService] 파일 인덱싱 실패", e);
      throw new BusinessException(ErrorCode.RAG_INDEXING_FAILED);
    }
  }

  /**
   * 파일 삭제 (파일명 기준)
   * - Lucene 삭제 + Qdrant best-effort 삭제(표준 metadata filename 기반)
   */
  public RagIndexResponseDto removeFiles(List<String> filenames) {
    try {
      if (filenames == null || filenames.isEmpty()) {
        return RagIndexResponseDto.deleted(0);
      }

      int totalRemoved = 0;
      for (String filename : filenames) {
        int removed = sparseStore.deleteByFilename(filename);
        totalRemoved += removed;
        log.info("[RagIndexService] Lucene 파일 삭제: {} ({}개 문서)", filename, removed);
      }

      // Dense(Qdrant)도 best-effort로 filename payload 기준 삭제 (⚠️ 같은 파일명이 다른 namespace에도
      // 있으면 같이 지워질 수 있음)
      try {
        deleteVectorsByFilenames(filenames);
      } catch (Exception ex) {
        log.warn("[RagIndexService] Qdrant filename delete 실패(best-effort). msg={}", ex.getMessage());
      }

      return RagIndexResponseDto.deleted(totalRemoved);

    } catch (Exception e) {
      log.error("[RagIndexService] 파일 삭제 실패", e);
      throw new BusinessException(ErrorCode.RAG_DELETE_FAILED);
    }
  }

  /**
   * 전체 인덱스 재구축
   * - Lucene clear + Qdrant collection clear(best-effort)
   */
  public RagIndexResponseDto rebuildAll() {
    try {
      log.info("[RagIndexService] 전체 인덱스 재구축 시작");

      int deleted = sparseStore.clearIndex();
      log.info("[RagIndexService] 기존 Lucene 인덱스 삭제 완료: {}개", deleted);

      // Dense(Qdrant)도 best-effort로 전체 삭제
      try {
        deleteAllVectors();
      } catch (Exception ex) {
        log.warn("[RagIndexService] Qdrant 전체 삭제 실패(best-effort). msg={}", ex.getMessage());
      }

      return RagIndexResponseDto.builder()
          .success(true)
          .message("인덱스가 초기화되었습니다. 문서를 다시 추가하세요.")
          .indexedFileCount(0)
          .indexedChunkCount(0)
          .failedFiles(List.of())
          .processingTimeSeconds(0.0)
          .completedAt(java.time.LocalDateTime.now())
          .build();

    } catch (Exception e) {
      log.error("[RagIndexService] 인덱스 재구축 실패", e);
      throw new BusinessException(ErrorCode.RAG_INDEXING_FAILED);
    }
  }

  /**
   * 네임스페이스 삭제
   * - Lucene + Qdrant best-effort 동기화
   */
  public RagIndexResponseDto deleteNamespace(String namespace) {
    try {
      log.info("[RagIndexService] 네임스페이스 삭제 - namespace: {}", namespace);

      int deleted = sparseStore.deleteByNamespace(namespace);
      log.info("[RagIndexService] Lucene 네임스페이스 삭제 완료: {}개 문서", deleted);

      try {
        deleteVectorsByNamespace(namespace);
      } catch (Exception ex) {
        log.warn("[RagIndexService] Qdrant namespace delete 실패(best-effort). namespace={}, msg={}",
            namespace, ex.getMessage());
      }

      return RagIndexResponseDto.deleted(deleted);

    } catch (Exception e) {
      log.error("[RagIndexService] 네임스페이스 삭제 실패", e);
      throw new BusinessException(ErrorCode.RAG_DELETE_FAILED);
    }
  }

  /**
   * status는 config 기반으로 표시
   */
  public RagIndexStatusResponseDto getStatus() {
    try {
      Map<String, Object> stats = sparseStore.getIndexStats();

      @SuppressWarnings("unchecked")
      Map<String, Integer> namespaceCounts = (Map<String, Integer>) stats.get("namespaceDocumentCounts");

      return RagIndexStatusResponseDto.builder()
          .ready(true)
          .totalDocuments((Integer) stats.get("totalDocuments"))
          .totalChunks((Integer) stats.get("totalDocuments")) // Lucene은 청크 단위 저장
          .namespaceDocumentCounts(namespaceCounts)
          .indexSizeMB((Double) stats.get("indexSizeMB"))
          .lastIndexedAt(java.time.LocalDateTime.now())
          .luceneIndexPath((String) stats.get("indexPath"))
          .qdrantCollection(resolveQdrantCollectionName())
          .vectorDimension(resolveVectorDimension())
          .build();

    } catch (Exception e) {
      log.error("[RagIndexService] 인덱스 상태 조회 실패", e);
      throw new BusinessException(ErrorCode.RAG_INDEX_NOT_READY);
    }
  }

  // =========================
  // Dense(Qdrant) indexing
  // =========================

  private record DenseIndexResult(int indexedChunks, List<String> failedFiles) {
  }

  /**
   * ✅ Dense 인덱싱 메타데이터 표준화 버전
   * - 모든 chunk에
   * filename/title/source/filePath/namespace/chunkIndex/chunkId/docId/ingestId/ingestedAt
   * 등을 강제
   */
  private DenseIndexResult indexDenseToVectorStore(List<Path> paths, String namespace) {
    int chunkSize = resolveChunkSize();
    int overlap = resolveChunkOverlap();

    String ingestId = UUID.randomUUID().toString();
    String ingestedAt = OffsetDateTime.now().toString();

    int totalAdded = 0;
    List<String> failed = new ArrayList<>();
    List<Document> batch = new ArrayList<>(VECTOR_ADD_BATCH_SIZE);

    for (Path p : paths) {
      try {
        if (p == null || !Files.exists(p) || !Files.isRegularFile(p)) {
          failed.add(String.valueOf(p));
          continue;
        }

        String filename = p.getFileName().toString();
        String fileUri = normalizeFileUri(p.toUri().toString()); // file:///... 로 통일
        String absPath = p.toAbsolutePath().toString();

        String text = extractTextFromPdf(p);
        if (text == null || text.isBlank()) {
          failed.add(filename);
          continue;
        }

        List<String> chunks = simpleChunks(text, chunkSize, overlap);
        String docId = stableDocId(namespace, absPath);

        for (int i = 0; i < chunks.size(); i++) {
          Map<String, Object> meta = standardDenseMetadata(
              namespace,
              filename,
              fileUri,
              absPath,
              docId,
              i,
              ingestId,
              ingestedAt);

          batch.add(new Document(chunks.get(i), meta));

          if (batch.size() >= VECTOR_ADD_BATCH_SIZE) {
            vector.add(batch);
            totalAdded += batch.size();
            batch.clear();
          }
        }

      } catch (Exception ex) {
        log.warn("[RagIndexService] Dense 인덱싱 실패: file={}, msg={}", p, ex.getMessage());
        failed.add(p != null ? p.getFileName().toString() : "null");
      }
    }

    if (!batch.isEmpty()) {
      vector.add(batch);
      totalAdded += batch.size();
      batch.clear();
    }

    log.info("[RagIndexService] Dense(Qdrant) indexing done: namespace={}, addedChunks={}, failedFiles={}",
        namespace, totalAdded, failed.size());

    return new DenseIndexResult(totalAdded, failed);
  }

  private Map<String, Object> standardDenseMetadata(
      String namespace,
      String filename,
      String sourceUri,
      String filePathAbs,
      String docId,
      int chunkIndex,
      String ingestId,
      String ingestedAt) {
    Map<String, Object> meta = new HashMap<>();

    // ✅ citations/retrieval용 핵심
    meta.put(Meta.NAMESPACE, namespace);
    meta.put(Meta.FILENAME, filename); // ✅ RagQueryService가 이걸 먼저 봄
    meta.put(Meta.TITLE, filename); // ✅ fallback 방지
    meta.put(Meta.SOURCE, sourceUri); // ✅ file:///... 형태 권장
    meta.put(Meta.SOURCE_NAME, filename);
    meta.put(Meta.FILE_PATH, filePathAbs);

    // ✅ chunk 관리
    meta.put(Meta.CHUNK_INDEX, chunkIndex);
    meta.put(Meta.CHUNK_ID, UUID.randomUUID().toString());
    meta.put(Meta.DOC_ID, docId);

    // ✅ 인덱싱 실행 컨텍스트
    meta.put(Meta.INGEST_ID, ingestId);
    meta.put(Meta.INGESTED_AT, ingestedAt);

    // ✅ 부가
    meta.put(Meta.MIME_TYPE, "application/pdf");

    return meta;
  }

  private String stableDocId(String namespace, String absPath) {
    // stable id: 동일 namespace + 동일 absPath면 항상 동일 docId
    String seed = namespace + "|" + absPath;
    return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
  }

  private String normalizeFileUri(String uri) {
    // Path.toUri()는 보통 file:///C:/... 이지만, 혹시 file:/C:/... 섞이면 정규화
    if (uri != null && uri.startsWith("file:/") && !uri.startsWith("file:///")) {
      return uri.replaceFirst("^file:/+", "file:///");
    }
    return uri;
  }

  private String extractTextFromPdf(Path pdfPath) throws Exception {
    try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      String text = stripper.getText(doc);
      return (text == null) ? "" : text.trim();
    }
  }

  // =========================
  // Qdrant delete helpers (REST, best-effort)
  // =========================

  private void deleteVectorsByNamespace(String namespace) throws Exception {
    String url = qdrantDeleteUrl();

    String body = """
        {
          "filter": {
            "must": [
              { "key": "%s", "match": { "value": "%s" } }
            ]
          }
        }
        """.formatted(Meta.NAMESPACE, escapeJson(namespace));

    qdrantPost(url, body, "Qdrant delete by namespace failed");
    log.info("[RagIndexService] Qdrant points deleted by namespace. collection={}, namespace={}",
        resolveQdrantCollectionName(), namespace);
  }

  private void deleteVectorsByFilenames(List<String> filenames) throws Exception {
    String url = qdrantDeleteUrl();

    // OR 조건은 should 사용
    String should = filenames.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(fn -> """
            { "key": "%s", "match": { "value": "%s" } }
            """.formatted(Meta.FILENAME, escapeJson(fn)))
        .collect(Collectors.joining(","));

    if (should.isBlank())
      return;

    String body = """
        {
          "filter": {
            "should": [
              %s
            ]
          }
        }
        """.formatted(should);

    qdrantPost(url, body, "Qdrant delete by filenames failed");
    log.info("[RagIndexService] Qdrant points deleted by filenames. collection={}, filenames={}",
        resolveQdrantCollectionName(), filenames.size());
  }

  private void deleteAllVectors() throws Exception {
    String url = qdrantDeleteUrl();

    // 전체 삭제: 빈 must 배열로 "모든 포인트 매칭" 의도 명확화
    String body = """
        {
          "filter": { "must": [] }
        }
        """;

    qdrantPost(url, body, "Qdrant delete all failed");
    log.info("[RagIndexService] Qdrant points deleted: ALL. collection={}", resolveQdrantCollectionName());
  }

  private String qdrantDeleteUrl() {
    String host = env.getProperty("spring.ai.vectorstore.qdrant.host", "localhost");
    String restPort = env.getProperty("QDRANT_REST_PORT", DEFAULT_QDRANT_REST_PORT);
    String collection = resolveQdrantCollectionName();
    return "http://" + host + ":" + restPort + "/collections/" + collection + "/points/delete?wait=true";
  }

  private void qdrantPost(String url, String body, String errPrefix) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
    if (res.statusCode() < 200 || res.statusCode() >= 300) {
      throw new IllegalStateException(errPrefix + ": status=" + res.statusCode() + ", body=" + res.body());
    }
  }

  private String escapeJson(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  // =========================
  // Helpers
  // =========================

  private List<Path> resolvePathsFromRequest(RagFileIndexRequestDto request) {
    List<String> filePaths = request.getFilePaths();
    if (filePaths == null || filePaths.isEmpty())
      return List.of();

    // ✅ 존재하는 파일만 통과
    return filePaths.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(Path::of)
        .filter(Files::exists)
        .filter(Files::isRegularFile)
        .collect(Collectors.toList());
  }

  private int resolveChunkSize() {
    Integer v = env.getProperty("rag.sparse.chunk.size", Integer.class, DEFAULT_CHUNK_SIZE);
    return (v == null || v <= 0) ? DEFAULT_CHUNK_SIZE : v;
  }

  private int resolveChunkOverlap() {
    Integer v = env.getProperty("rag.sparse.chunk.overlap", Integer.class, DEFAULT_CHUNK_OVERLAP);
    if (v == null)
      return DEFAULT_CHUNK_OVERLAP;
    return Math.max(v, 0);
  }

  private String resolveQdrantCollectionName() {
    String v = env.getProperty("spring.ai.vectorstore.qdrant.collection-name");
    if (v != null && !v.isBlank())
      return v;

    v = env.getProperty("spring.ai.vectorstore.qdrant.collectionName");
    if (v != null && !v.isBlank())
      return v;

    v = env.getProperty("QDRANT_COLLECTION");
    if (v != null && !v.isBlank())
      return v;

    return "vector_store";
  }

  private int resolveVectorDimension() {
    String model = env.getProperty("spring.ai.openai.embedding.options.model");
    if (model == null || model.isBlank()) {
      model = env.getProperty("SPRING_AI_OPENAI_EMBEDDING_OPTIONS_MODEL",
          env.getProperty("OPENAI_EMBEDDING_MODEL", ""));
    }

    if ("text-embedding-3-large".equalsIgnoreCase(model))
      return 3072;
    if ("text-embedding-3-small".equalsIgnoreCase(model))
      return 1536;

    // 알 수 없으면 안전하게 small로 fallback
    return 1536;
  }

  private List<String> simpleChunks(String text, int size, int overlap) {
    var out = new ArrayList<String>();
    if (text == null)
      return out;

    String t = text.trim();
    if (t.isEmpty())
      return out;

    int safeSize = Math.max(size, 1);
    // overlap이 size 이상이면 무한루프 위험 -> 방어
    int safeOverlap = Math.min(Math.max(overlap, 0), Math.max(safeSize - 1, 0));

    int i = 0, n = t.length();
    while (i < n) {
      int end = Math.min(n, i + safeSize);
      String chunk = t.substring(i, end).trim();
      if (!chunk.isEmpty())
        out.add(chunk);

      if (end == n)
        break;

      i = end - safeOverlap;
      if (i < 0)
        i = 0;
    }
    return out;
  }
}
