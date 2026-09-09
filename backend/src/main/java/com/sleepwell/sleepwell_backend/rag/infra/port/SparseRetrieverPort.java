package com.sleepwell.sleepwell_backend.rag.infra.port;

import com.sleepwell.sleepwell_backend.rag.model.ScoredDoc;
import java.util.List;

public interface SparseRetrieverPort {
    List<ScoredDoc> search(String query, int topK);
}
