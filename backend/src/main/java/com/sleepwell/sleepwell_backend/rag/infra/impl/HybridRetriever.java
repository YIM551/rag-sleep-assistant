package com.sleepwell.sleepwell_backend.rag.infra.impl;

import com.sleepwell.sleepwell_backend.rag.infra.port.DenseRetrieverPort;
import com.sleepwell.sleepwell_backend.rag.infra.port.SparseRetrieverPort;
import com.sleepwell.sleepwell_backend.rag.model.ScoredDoc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HybridRetriever {

    private final DenseRetrieverPort dense;
    private final SparseRetrieverPort sparse;
    private final int rrfK;

    public HybridRetriever(
            DenseRetrieverPort dense,
            SparseRetrieverPort sparse,
            @Value("${rag.retrieval.hybrid.rrfK:60}") int rrfK) {
        this.dense = dense;
        this.sparse = sparse;
        this.rrfK = rrfK;
    }

    /**
     * 하이브리드 검색:
     * 1) dense / sparse 각각 검색
     * 2) 점수 정규화 후 RRF로 1차 융합
     * 3) useMmr=true면 MMR로 다양성 리랭킹
     */
    public List<ScoredDoc> search(
            String query,
            int denseTopK,
            int sparseTopK,
            boolean useMmr,
            int mmrK,
            double lambda) {
        // 1. dense / sparse 각각 검색
        var denseResults = dense.search(query, denseTopK);
        var sparseResults = sparse.search(query, sparseTopK);

        // 2. 점수 정규화
        ScoringUtils.normalize(denseResults);
        ScoringUtils.normalize(sparseResults);

        // 3. RRF 융합
        List<ScoredDoc> fused = ScoringUtils.rrfFuse(denseResults, sparseResults, rrfK);

        // 4. MMR 다양성 리랭킹 (옵션)
        if (useMmr && !fused.isEmpty()) {
            int k = Math.min(mmrK, fused.size());
            return ScoringUtils.mmr(fused, k, lambda);
        }

        return fused;
    }
}
