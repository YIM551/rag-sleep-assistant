package com.sleepwell.sleepwell_backend.rag.infra.impl;

import com.sleepwell.sleepwell_backend.rag.infra.Reranker;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * Maximal Marginal Relevance 기반 재랭커.
 * mmrScore = λ * relevance - (1 - λ) * max_sim(candidate, selected)
 * - relevance: 호출자가 넘긴 scoreFn
 * - similarity: 간단한 Jaccard(토큰 집합) 유사도
 */
@Component
public class MmrReranker implements Reranker {

    @Override
    public <T> List<T> mmr(List<T> items, int topK, ToDoubleFunction<T> scoreFn, double lambda) {
        if (items == null || items.isEmpty() || topK <= 0)
            return List.of();

        final int K = Math.min(topK, items.size());
        final double lam = Math.max(0.0, Math.min(1.0, lambda));

        // 작업용 목록 & 사전 계산
        List<T> pool = new ArrayList<>(items);
        List<T> selected = new ArrayList<>(K);

        // 미리 relevance와 토큰 캐시 계산
        double[] rel = new double[pool.size()];
        List<Set<String>> tokenCache = new ArrayList<>(pool.size());
        for (int i = 0; i < pool.size(); i++) {
            T it = pool.get(i);
            rel[i] = scoreFn.applyAsDouble(it);
            tokenCache.add(tokensFrom(it));
        }

        boolean[] used = new boolean[pool.size()];

        for (int step = 0; step < K; step++) {
            int bestIdx = -1;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < pool.size(); i++) {
                if (used[i])
                    continue;

                double redundancy = 0.0;
                if (!selected.isEmpty()) {
                    redundancy = maxSimilarity(tokenCache.get(i), selected, pool, tokenCache);
                }

                double mmrScore = lam * rel[i] - (1.0 - lam) * redundancy;
                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    bestIdx = i;
                }
            }

            if (bestIdx < 0)
                break;
            used[bestIdx] = true;
            selected.add(pool.get(bestIdx));
        }

        return selected;
    }

    // 선택된 항목들과의 최대 유사도
    private <T> double maxSimilarity(Set<String> candTokens, List<T> selected, List<T> pool,
            List<Set<String>> tokenCache) {
        double max = 0.0;
        for (T sel : selected) {
            int idx = pool.indexOf(sel); // n이 작으니 단순 O(n)로 처리
            Set<String> selTokens = idx >= 0 ? tokenCache.get(idx) : tokensFrom(sel);
            double sim = jaccard(candTokens, selTokens);
            if (sim > max)
                max = sim;
        }
        return max;
    }

    // Jaccard 유사도
    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty())
            return 0.0;
        int inter = 0;
        if (a.size() < b.size()) {
            for (String s : a)
                if (b.contains(s))
                    inter++;
        } else {
            for (String s : b)
                if (a.contains(s))
                    inter++;
        }
        int union = a.size() + b.size() - inter;
        return union == 0 ? 0.0 : ((double) inter) / union;
    }

    private static final Set<String> STOP = Set.of(
            "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "with", "by", "at",
            "is", "are", "was", "were", "be", "as", "that", "this", "it", "from", "we", "you");

    // 아이템에서 텍스트 추출 → 토큰 집합 만들기
    private <T> Set<String> tokensFrom(T item) {
        String text = extractText(item);
        if (text == null)
            text = String.valueOf(item);
        text = text.toLowerCase(Locale.ROOT);
        String[] parts = text.replaceAll("[^\\p{L}\\p{N}]+", " ").trim().split("\\s+");

        Set<String> out = new HashSet<>();
        for (String p : parts) {
            if (p.length() < 2)
                continue;
            if (STOP.contains(p))
                continue;
            out.add(p);
        }
        return out;
    }

    /**
     * 문서 본문을 최대한 끌어내려는 노력:
     * 1) record accessor "doc()" 시도 → 대상 객체로 바꿔서
     * 2) getContent()/content() 호출
     * 3) getMetadata().get("text" or "content")
     * 실패 시 toString()
     */
    private String extractText(Object o) {
        if (o == null)
            return "";

        Object target = o;
        try {
            Method m = o.getClass().getMethod("doc"); // e.g., record Scored(doc, score)
            target = m.invoke(o);
        } catch (Exception ignore) {
            /* no-op */ }

        String s = tryCallString(target, "getContent");
        if (!s.isEmpty())
            return s;

        s = tryCallString(target, "content");
        if (!s.isEmpty())
            return s;

        try {
            Method getMetadata = target.getClass().getMethod("getMetadata");
            Object map = getMetadata.invoke(target);
            if (map instanceof Map<?, ?> md) {
                Object t = md.get("text");
                if (t == null)
                    t = md.get("content");
                if (t != null)
                    return String.valueOf(t);
            }
        } catch (Exception ignore) {
            /* no-op */ }

        return String.valueOf(target);
    }

    private String tryCallString(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            Object v = m.invoke(target);
            return v == null ? "" : String.valueOf(v);
        } catch (Exception e) {
            return "";
        }
    }
}
