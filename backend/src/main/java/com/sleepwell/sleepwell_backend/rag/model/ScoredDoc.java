package com.sleepwell.sleepwell_backend.rag.model;

import java.util.Map;

public record ScoredDoc(String id, String content, double score, String source, Map<String,Object> meta) {
    public ScoredDoc withScore(double s) { return new ScoredDoc(id, content, s, source, meta); }
    public String metaString(String key) {
        if (meta == null) return null;
        Object v = meta.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
