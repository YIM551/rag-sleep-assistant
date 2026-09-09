package com.sleepwell.sleepwell_backend.rag.service;

import com.sleepwell.sleepwell_backend.rag.infra.PdfUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfIngestService {

    private final VectorStore vector;

    /** 한 번에 임베딩 요청할 문서 수(너무 크면 응답 중 연결이 끊길 수 있음) */
    private static final int EMBED_BATCH_SIZE = 16; // 8~32 추천
    /** 배치 실패 시 재시도 횟수 */
    private static final int RETRIES = 2;
    /** 재시도 사이 대기(ms) */
    private static final long RETRY_DELAY_MS = 400L;

    /** 폴더 내 PDF 전부 읽어 분할 후 VectorStore 적재 */
    public int indexFolder(String folderPath) throws Exception {
        Path dir = Paths.get(folderPath);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Not a directory: " + folderPath);
        }

        List<Document> all = new ArrayList<>();
        try (var stream = Files.walk(dir)) {
            List<Path> pdfs = stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                    .collect(Collectors.toList());

            if (pdfs.isEmpty()) {
                log.warn("No PDF files found in {}", folderPath);
            }

            for (Path p : pdfs) {
                List<Document> docs = PdfUtils.read(p.toString()); // PDF -> Document 리스트
                // 토큰 단위 분할 (chunk 800, minTokens 200, overlap 120, 최소 문장수 3, 구분자 미포함)
                var splitter = new TokenTextSplitter(800, 200, 120, 3, false);
                List<Document> chunks = splitter.apply(docs);

                for (Document d : chunks) {
                    d.getMetadata().putIfAbsent("filename", p.getFileName().toString());
                    d.getMetadata().putIfAbsent("source", p.toUri().toString());
                }
                log.info("Split {} into {} chunks", p.getFileName(), chunks.size());
                all.addAll(chunks);
            }
        }

        if (!all.isEmpty()) {
            log.info("Total chunks to embed: {}", all.size());
            addInBatches(all);
        } else {
            log.info("Nothing to index: no chunks produced.");
        }

        return all.size();
    }

    /** 큰 리스트를 작은 배치로 나눠 안정적으로 적재 */
    private void addInBatches(List<Document> docs) {
        for (int i = 0; i < docs.size(); i += EMBED_BATCH_SIZE) {
            int end = Math.min(i + EMBED_BATCH_SIZE, docs.size());
            List<Document> batch = docs.subList(i, end);

            // 간단 재시도 로직
            int attempt = 0;
            while (true) {
                try {
                    vector.add(batch);
                    break; // 성공
                } catch (Exception e) {
                    attempt++;
                    if (attempt > RETRIES) {
                        // 마지막 시도도 실패하면 상위로 던져 전체 인덱싱을 중단(로그에 위치 남김)
                        log.error("Failed to add batch [{}-{}]/{} after {} retries", i, end, docs.size(), RETRIES, e);
                        throw e;
                    }
                    log.warn("Batch [{}-{}]/{} failed (attempt {}/{}). Retrying in {} ms. Cause: {}",
                            i, end, docs.size(), attempt, RETRIES, RETRY_DELAY_MS, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ignored) {
                        /* no-op */ }
                }
            }

            // 서버/네트워크 부하 완화용 소폭 딜레이(선택)
            try {
                Thread.sleep(200L);
            } catch (InterruptedException ignored) {
                /* no-op */ }
        }
    }
}
