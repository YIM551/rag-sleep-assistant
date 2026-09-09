package com.sleepwell.sleepwell_backend.rag.infra;

import java.io.IOException;
import com.sleepwell.sleepwell_backend.rag.infra.extract.DocExtractor;
import com.sleepwell.sleepwell_backend.rag.infra.extract.DocExtractor.ExtractedPart;
import com.sleepwell.sleepwell_backend.rag.model.ScoredDoc;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.index.MultiFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SparseStore {

  private static final Logger log = LoggerFactory.getLogger(SparseStore.class);

  /* ---- 설정 ---- */
  @Value("${rag.sparse.indexPath:./build/rag-lucene}")
  private String indexPath;

  @Value("${rag.sparse.corpusPath:}")
  private String corpusPath;

  @Value("${rag.sparse.rebuildOnStart:true}")
  private boolean rebuildOnStart;

  @Value("${rag.sparse.chunk.size:1200}")
  private int chunkSize;

  @Value("${rag.sparse.chunk.overlap:200}")
  private int chunkOverlap;

  @Value("${rag.sparse.namespace.fromPath:false}")
  private boolean namespaceFromPath;

  /* ---- 루씬 리소스 ---- */
  private Directory directory;
  private IndexWriter writer;
  private DirectoryReader reader;
  private IndexSearcher searcher;

  // 분석기
  private final Analyzer std = new StandardAnalyzer();
  private final Analyzer nori = new KoreanAnalyzer();
  private Analyzer perFieldAnalyzer;

  // 하이라이트용 필드타입(스토어 + term vectors pos/offset)
  private final FieldType textFieldType;

  private final DocExtractor extractor;

  public SparseStore(DocExtractor extractor) {
    this.extractor = extractor;
    FieldType ft = new FieldType(TextField.TYPE_STORED);
    ft.setStoreTermVectors(true);
    ft.setStoreTermVectorPositions(true);
    ft.setStoreTermVectorOffsets(true);
    this.textFieldType = ft;
  }

  @PostConstruct
  public void init() throws Exception {
    Files.createDirectories(Path.of(indexPath));
    directory = FSDirectory.open(Path.of(indexPath));

    Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
    fieldAnalyzers.put("title", nori);
    fieldAnalyzers.put("text", nori);
    perFieldAnalyzer = new PerFieldAnalyzerWrapper(std, fieldAnalyzers);

    IndexWriterConfig iwc = new IndexWriterConfig(perFieldAnalyzer);
    // 항상 CREATE_OR_APPEND: 증분 인덱싱, 볼륨 낭비 방지
    iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    iwc.setSimilarity(new BM25Similarity());

    writer = new IndexWriter(directory, iwc);

    // rebuildOnStart 제거: 관리자 API로만 수동 인덱싱
    // 서버 재시작해도 기존 인덱스 유지

    reader = DirectoryReader.open(writer);
    searcher = new IndexSearcher(reader);
    searcher.setSimilarity(new BM25Similarity());
    log.info("[SparseStore] index ready: docs={}", countDocs());
  }

  @PreDestroy
  public void close() {
    try {
      if (reader != null)
        reader.close();
    } catch (Exception ignore) {
    }
    try {
      if (writer != null)
        writer.close();
    } catch (Exception ignore) {
    }
    try {
      if (directory != null)
        directory.close();
    } catch (Exception ignore) {
    }
  }

  /* ---------------- 공개 API: 검색(+ 하이라이트) ---------------- */

  /** BM25 검색 (title^2.0 + text^1.0) + UnifiedHighlighter 스니펫 */
  public List<ScoredDoc> search(String rawQuery, int topK) {
    try {
      if (rawQuery == null || rawQuery.isBlank())
        return List.of();
      refreshReaderIfChanged();

      String safe = QueryParserBase.escape(rawQuery);
      log.debug("[SparseStore] input length={}, normalized length={}", rawQuery.length(), safe.length());

      Map<String, Float> boosts = Map.of("title", 2.0f, "text", 1.0f);
      MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[] { "title", "text" }, perFieldAnalyzer,
          boosts);
      Query q = parser.parse(safe);

      TopDocs td = searcher.search(q, Math.max(1, topK));

      // 하이라이터 준비 (필드: text, 문서당 1개 스니펫)
      UnifiedHighlighter uh = new UnifiedHighlighter(searcher, perFieldAnalyzer);
      uh.setMaxLength(20_000);
      String[] snippets = uh.highlight("text", q, td, 1);

      List<ScoredDoc> out = new ArrayList<>(td.scoreDocs.length);
      for (int i = 0; i < td.scoreDocs.length; i++) {
        ScoreDoc sd = td.scoreDocs[i];
        org.apache.lucene.document.Document d = searcher.doc(sd.doc);

        String id = nvl(d.get("id"), UUID.randomUUID().toString());
        String title = nvl(d.get("title"), "");
        String text = nvl(d.get("text"), "");
        String source = nvl(d.get("source"), "");
        String filename = nvl(d.get("filename"), "");
        String namespace = nvl(d.get("namespace"), "default");
        String pageStr = d.get("page"); // null 가능
        Integer page = (pageStr == null) ? null : Integer.valueOf(pageStr);

        String snippet = null;
        if (snippets != null && i < snippets.length && snippets[i] != null) {
          snippet = snippets[i].replaceAll("\\s+", " ").trim();
        }
        if (snippet == null || snippet.isBlank()) {
          snippet = pickQuote(text);
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", title);
        meta.put("filename", filename);
        meta.put("source", source);
        meta.put("namespace", namespace);
        if (page != null)
          meta.put("page", page);
        meta.put("snippet", snippet);

        out.add(new ScoredDoc(id, text, sd.score, source, meta));
      }
      return out;
    } catch (Exception e) {
      log.warn("[SparseStore] search error: {}", e.toString());
      return List.of();
    }
  }

  /* ---------------- 인덱싱 (Tika 추출 사용) ---------------- */

  /** corpusPath 폴더 전체를 순회하며 파일별 추출기(DocExtractor)로 텍스트 수집 → 페이지/청크 단위 색인 */
  private int indexCorpus(Path root) throws IOException {
    if (!Files.isDirectory(root)) {
      log.warn("[SparseStore] corpusPath not a directory: {}", root);
      return 0;
    }

    List<Path> files;
    try (var walk = Files.walk(root)) {
      files = walk.filter(Files::isRegularFile).collect(Collectors.toList());
    }
    log.info("[SparseStore] indexing start: dir='{}' files={}", root.toAbsolutePath(), files.size());

    int added = 0;
    for (Path p : files) {
      String filename = p.getFileName().toString();
      String idBase = p.toAbsolutePath().toString();
      String namespace = deriveNamespace(root, p);
      String source = p.toUri().toString();
      String title = filename;

      List<ExtractedPart> parts;
      try {
        parts = extractor.extract(p);
      } catch (Exception ex) {
        log.warn("[SparseStore] extract failed: {} -> {}", p, ex.toString());
        continue;
      }
      if (parts == null || parts.isEmpty())
        continue;

      int chunkIdxGlobal = 0;
      for (ExtractedPart part : parts) {
        String body = clean(part.text());
        if (body.isBlank())
          continue;

        // 페이지 텍스트를 청크로 쪼개기
        List<String> chunks = chunk(body, chunkSize, chunkOverlap);
        int idx = 0;
        for (String c : chunks) {
          org.apache.lucene.document.Document d = new org.apache.lucene.document.Document();
          String chunkId = idBase + "#" + chunkIdxGlobal;
          d.add(new StringField("id", chunkId, Field.Store.YES));
          d.add(new TextField("title", title, Field.Store.YES));

          // 하이라이트를 위해 term vectors 저장된 타입 사용
          d.add(new Field("text", c, textFieldType));

          d.add(new StringField("filename", filename, Field.Store.YES));
          d.add(new StringField("source", source, Field.Store.YES));
          d.add(new StringField("namespace", namespace, Field.Store.YES));

          if (part.page() != null) {
            d.add(new StoredField("page", part.page())); // 페이지 번호 저장
          }

          // updateDocument() 사용: 중복 자동 방지 (upsert)
          // 같은 id가 있으면 삭제 후 추가, 없으면 새로 추가
          writer.updateDocument(new Term("id", chunkId), d);
          idx++;
          chunkIdxGlobal++;
          added++;
        }
      }
    }
    return added;
  }

  private void refreshReaderIfChanged() throws IOException {
    DirectoryReader newReader = DirectoryReader.openIfChanged(reader, writer);
    if (newReader != null) {
      reader.close();
      reader = newReader;
      searcher = new IndexSearcher(reader);
      searcher.setSimilarity(new BM25Similarity());
      log.debug("[SparseStore] reader refreshed: docs={}", countDocs());
    }
  }

  private int countDocs() {
    try {
      return writer.getDocStats().numDocs;
    } catch (Exception e) {
      return -1;
    }
  }

  private String deriveNamespace(Path root, Path file) {
    if (!namespaceFromPath)
      return "default";
    try {
      Path rel = root.relativize(file).normalize();
      if (rel.getNameCount() >= 2) {
        return rel.getName(0).toString();
      }
    } catch (Exception ignore) {
    }
    return "default";
  }

  private static List<String> chunk(String text, int size, int overlap) {
    if (text == null || text.isBlank())
      return List.of();
    List<String> out = new ArrayList<>();
    int start = 0, n = text.length();
    while (start < n) {
      int end = Math.min(n, start + size);
      out.add(text.substring(start, end));
      if (end == n)
        break;
      start = Math.max(end - overlap, start + 1);
    }
    return out;
  }

  private static String clean(String s) {
    if (s == null)
      return "";
    return s.replaceAll("\\s+", " ").trim();
  }

  private static String nvl(String s, String def) {
    return (s == null) ? def : s;
  }

  // 인용문(폴백 스니펫) 280자
  private static String pickQuote(String text) {
    if (text == null)
      return "";
    String t = text
        .replaceAll("-\\s*\\n", "")
        .replaceAll("-\\s+", "")
        .replaceAll("\\s*\\n\\s*", " ")
        .replaceAll("\\s+", " ")
        .trim();
    return t.length() > 280 ? t.substring(0, 280) + "…" : t;
  }

  /* ---------------- 증분 인덱싱 API (관리자용) ---------------- */

  /**
   * 특정 파일들을 인덱싱
   * @param filePaths rag-corpus 상대 경로 목록
   * @param namespace 네임스페이스 (null이면 경로에서 자동 파생)
   * @return 인덱싱된 문서 수
   */
  public int indexFiles(List<Path> filePaths, String namespace) throws IOException {
    if (filePaths == null || filePaths.isEmpty()) {
      return 0;
    }

    Path corpusRoot = (corpusPath != null && !corpusPath.isBlank())
        ? Path.of(corpusPath)
        : Path.of("./rag-corpus");

    int added = 0;
    for (Path relativePath : filePaths) {
      Path absolutePath = corpusRoot.resolve(relativePath).normalize();

      if (!Files.exists(absolutePath)) {
        log.warn("[SparseStore] 파일을 찾을 수 없습니다: {}", absolutePath);
        continue;
      }

      String filename = absolutePath.getFileName().toString();
      String idBase = absolutePath.toAbsolutePath().toString();
      String ns = (namespace != null && !namespace.isBlank())
          ? namespace
          : deriveNamespace(corpusRoot, absolutePath);
      String source = absolutePath.toUri().toString();
      String title = filename;

      List<ExtractedPart> parts;
      try {
        parts = extractor.extract(absolutePath);
      } catch (Exception ex) {
        log.warn("[SparseStore] 추출 실패: {} -> {}", absolutePath, ex.toString());
        continue;
      }

      if (parts == null || parts.isEmpty()) {
        continue;
      }

      int chunkIdxGlobal = 0;
      for (ExtractedPart part : parts) {
        String body = clean(part.text());
        if (body.isBlank()) {
          continue;
        }

        List<String> chunks = chunk(body, chunkSize, chunkOverlap);
        for (String c : chunks) {
          org.apache.lucene.document.Document d = new org.apache.lucene.document.Document();
          d.add(new StringField("id", idBase + "#" + chunkIdxGlobal, Field.Store.YES));
          d.add(new TextField("title", title, Field.Store.YES));
          d.add(new Field("text", c, textFieldType));
          d.add(new StringField("filename", filename, Field.Store.YES));
          d.add(new StringField("source", source, Field.Store.YES));
          d.add(new StringField("namespace", ns, Field.Store.YES));

          if (part.page() != null) {
            d.add(new StoredField("page", part.page()));
          }

          writer.addDocument(d);
          chunkIdxGlobal++;
          added++;
        }
      }
    }

    writer.commit();
    refreshReaderIfChanged();
    log.info("[SparseStore] indexFiles: added={}", added);
    return added;
  }

  /**
   * 특정 파일명의 모든 문서 삭제
   * @param filename 파일명
   * @return 삭제된 문서 수
   */
  public int deleteByFilename(String filename) throws IOException {
    if (filename == null || filename.isBlank()) {
      return 0;
    }

    Query q = new TermQuery(new Term("filename", filename));
    long deletedLong = writer.deleteDocuments(q);
    writer.commit();
    refreshReaderIfChanged();
    int deleted = (int) deletedLong;
    log.info("[SparseStore] deleteByFilename: filename='{}' deleted={}", filename, deleted);
    return deleted;
  }

  /**
   * 특정 네임스페이스의 모든 문서 삭제
   * @param namespace 네임스페이스
   * @return 삭제된 문서 수
   */
  public int deleteByNamespace(String namespace) throws IOException {
    if (namespace == null || namespace.isBlank()) {
      return 0;
    }

    Query q = new TermQuery(new Term("namespace", namespace));
    long deletedLong = writer.deleteDocuments(q);
    writer.commit();
    refreshReaderIfChanged();
    int deleted = (int) deletedLong;
    log.info("[SparseStore] deleteByNamespace: namespace='{}' deleted={}", namespace, deleted);
    return deleted;
  }

  /**
   * 전체 인덱스 삭제
   * @return 삭제된 문서 수
   */
  public int clearIndex() throws IOException {
    int beforeCount = countDocs();
    writer.deleteAll();
    writer.commit();
    refreshReaderIfChanged();
    log.info("[SparseStore] clearIndex: deleted={}", beforeCount);
    return beforeCount;
  }

  /**
   * 인덱스 상태 정보 조회
   */
  public Map<String, Object> getIndexStats() throws IOException {
    refreshReaderIfChanged();

    int totalDocs = countDocs();
    Map<String, Integer> namespaceCounts = new HashMap<>();

    // 전체 문서를 순회하며 네임스페이스별 카운트
    // IndexReader는 여러 세그먼트를 가질 수 있으므로, 각 리프를 순회
    for (LeafReaderContext leaf : reader.leaves()) {
      LeafReader leafReader = leaf.reader();
      Bits liveDocs = leafReader.getLiveDocs();

      for (int i = 0; i < leafReader.maxDoc(); i++) {
        if (liveDocs != null && !liveDocs.get(i)) {
          continue; // 삭제된 문서는 건너뛰기
        }
        org.apache.lucene.document.Document doc = leafReader.document(i);
        String ns = nvl(doc.get("namespace"), "default");
        namespaceCounts.merge(ns, 1, Integer::sum);
      }
    }

    // 인덱스 크기 (대략)
    long dirSize = 0;
    try {
      Path indexDir = Path.of(indexPath);
      if (Files.exists(indexDir)) {
        dirSize = Files.walk(indexDir)
            .filter(Files::isRegularFile)
            .mapToLong(p -> {
              try {
                return Files.size(p);
              } catch (IOException e) {
                return 0;
              }
            })
            .sum();
      }
    } catch (Exception e) {
      log.warn("[SparseStore] 인덱스 크기 계산 실패: {}", e.toString());
    }

    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("totalDocuments", totalDocs);
    stats.put("namespaceDocumentCounts", namespaceCounts);
    stats.put("indexSizeMB", dirSize / (1024.0 * 1024.0));
    stats.put("indexPath", indexPath);

    return stats;
  }
}
