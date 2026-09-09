package com.sleepwell.sleepwell_backend.rag.infra.adapter;

import com.sleepwell.sleepwell_backend.rag.infra.port.DenseRetrieverPort;
import com.sleepwell.sleepwell_backend.rag.model.ScoredDoc;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class DenseAdapter implements DenseRetrieverPort {
    private final VectorStore vectorStore;

    public DenseAdapter(VectorStore vectorStore) {
        this.vectorStore = Objects.requireNonNull(vectorStore);
    }

    @Override
    public List<ScoredDoc> search(String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            throw new IllegalArgumentException("query must not be blank and topK must be positive");
        }
        var documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query).topK(topK).build());
        if (documents == null) {
            throw new IllegalStateException("VectorStore returned null results");
        }
        List<ScoredDoc> results = new ArrayList<>(documents.size());
        for (var document : documents) {
            var metadata = new HashMap<String, Object>(document.getMetadata());
            String source = Objects.toString(metadata.get("source"),
                    Objects.toString(metadata.get("url"), "dense"));
            results.add(new ScoredDoc(document.getId(), document.getText(),
                    document.getScore() == null ? 0.0 : document.getScore(), source, metadata));
        }
        return results;
    }
}
