package com.sleepwell.sleepwell_backend.rag.infra.impl;

import com.sleepwell.sleepwell_backend.rag.infra.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SpringAiEmbeddingClient implements EmbeddingClient {

    private final EmbeddingModel embeddingModel;

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty())
            return List.of();

        EmbeddingResponse res = embeddingModel.embedForResponse(texts);

        List<float[]> out = new ArrayList<>(res.getResults().size());
        res.getResults().forEach(r -> {
            float[] vec = r.getOutput(); // 현재 사용 중인 Spring AI 버전: float[] 반환
            if (vec != null) {
                // 방어적 복사(선택): 벡터를 외부에서 변경 못 하게
                out.add(Arrays.copyOf(vec, vec.length));
            }
        });
        return out;
    }
}
