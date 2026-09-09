package com.sleepwell.sleepwell_backend.rag.infra;

import java.util.List;

public interface EmbeddingClient {
    List<float[]> embed(List<String> texts) throws Exception;
}

