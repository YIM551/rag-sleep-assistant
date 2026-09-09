package com.sleepwell.sleepwell_backend.rag.infra.port;

import com.sleepwell.sleepwell_backend.rag.model.ScoredDoc;
import java.util.List;

public interface RerankerPort {
    List<ScoredDoc> rerank(String query, List<ScoredDoc> candidates, int topK);
}
