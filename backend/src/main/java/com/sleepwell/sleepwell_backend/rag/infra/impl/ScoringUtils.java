package com.sleepwell.sleepwell_backend.rag.infra.impl;

import com.sleepwell.sleepwell_backend.rag.model.ScoredDoc;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ScoringUtils {

    // ==================== RRF (Rank Reciprocal Fusion) ====================

    public static List<ScoredDoc> rrfFuse(List<ScoredDoc> a, List<ScoredDoc> b, int k) {
        Map<String, Double> r = new HashMap<>();
        int rank = 1;
        for (var d : a) {
            r.merge(d.id(), 1.0 / (k + rank++), Double::sum);
        }
        rank = 1;
        for (var d : b) {
            r.merge(d.id(), 1.0 / (k + rank++), Double::sum);
        }
        Map<String, ScoredDoc> any = new LinkedHashMap<>();
        a.forEach(d -> any.putIfAbsent(d.id(), d));
        b.forEach(d -> any.putIfAbsent(d.id(), d));
        return any.values().stream()
                .map(d -> d.withScore(r.getOrDefault(d.id(), 0.0)))
                .sorted(Comparator.comparing(ScoredDoc::score).reversed())
                .collect(Collectors.toList());
    }

    public static void normalize(List<ScoredDoc> docs) {
        if (docs.isEmpty())
            return;
        double min = docs.stream().mapToDouble(ScoredDoc::score).min().orElse(0);
        double max = docs.stream().mapToDouble(ScoredDoc::score).max().orElse(1);
        double diff = Math.max(1e-9, max - min);
        for (int i = 0; i < docs.size(); i++) {
            var d = docs.get(i);
            docs.set(i, d.withScore((d.score() - min) / diff));
        }
    }

    // ==================== MMR (Maximal Marginal Relevance) ====================

    /**
     * MMR 기반 다양성 리랭킹
     *
     * @param candidates RRF 등으로 1차 정렬된 문서 리스트 (상위 순서일수록 관련도가 높다고 가정)
     * @param k          최종으로 뽑을 문서 개수
     * @param lambda     관련도 vs 다양성 트레이드오프 (0~1, 1에 가까울수록 관련도 위주)
     */
    public static List<ScoredDoc> mmr(List<ScoredDoc> candidates, int k, double lambda) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        if (k <= 0) {
            return List.of();
        }

        int size = candidates.size();
        k = Math.min(k, size);

        // 랭크 기반 관련도: rank가 앞일수록 큰 값 (1 / (rank+1))
        Map<ScoredDoc, Double> relevance = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ScoredDoc d = candidates.get(i);
            relevance.put(d, 1.0 / (i + 1));
        }

        List<ScoredDoc> selected = new ArrayList<>();
        Set<ScoredDoc> remaining = new LinkedHashSet<>(candidates);

        while (selected.size() < k && !remaining.isEmpty()) {
            ScoredDoc best = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (ScoredDoc d : remaining) {
                double rel = relevance.getOrDefault(d, 0.0);
                double diversityPenalty = 0.0;

                if (!selected.isEmpty()) {
                    double maxSim = 0.0;
                    for (ScoredDoc s : selected) {
                        double sim = textSimilarity(d, s);
                        if (sim > maxSim) {
                            maxSim = sim;
                        }
                    }
                    diversityPenalty = maxSim;
                }

                double mmrScore = lambda * rel - (1.0 - lambda) * diversityPenalty;

                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    best = d;
                }
            }

            selected.add(best);
            remaining.remove(best);
        }

        return selected;
    }

    // ==================== RBF 기반 고급 리랭킹 ====================

    /**
     * RBF 형태의 점수 결합으로 query-문서 유사도와 1차 랭크를 함께 사용하는 고급 리랭킹.
     *
     * - baseRelevance: 1 / (rank+1) (RRF 결과에서의 순위 기반)
     * - textSim : 질의와 문서 간 Jaccard 유사도
     * - 최종 점수 : alpha * baseRelevance + (1-alpha) * RBF(1 - textSim)
     */
    public static List<ScoredDoc> rbfRerank(String query, List<ScoredDoc> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<ScoredDoc> list = new ArrayList<>(candidates);
        int size = list.size();
        if (topK <= 0 || topK > size) {
            topK = size;
        }

        // 랭크 기반 관련도 (1 / (rank+1))
        Map<ScoredDoc, Double> rel = new HashMap<>();
        for (int i = 0; i < size; i++) {
            rel.put(list.get(i), 1.0 / (i + 1));
        }

        Set<String> qTokens = tokenize(query);

        Map<ScoredDoc, Double> finalScore = new HashMap<>();
        double gamma = 2.0; // RBF 폭 (클수록 피크가 좁아짐)
        double alpha = 0.7; // 관련도 vs RBF 가중치

        for (ScoredDoc d : list) {
            double baseRel = rel.getOrDefault(d, 0.0);
            double sim = qTokens.isEmpty() ? 0.0 : jaccard(qTokens, tokenize(d.content()));
            double dist = 1.0 - sim; // 유사도가 높을수록 dist는 0에 가까움
            double rbf = Math.exp(-gamma * dist * dist);
            double score = alpha * baseRel + (1.0 - alpha) * rbf;
            finalScore.put(d, score);
        }

        list.sort((a, b) -> Double.compare(
                finalScore.getOrDefault(b, 0.0),
                finalScore.getOrDefault(a, 0.0)));

        return new ArrayList<>(list.subList(0, topK));
    }

    // ==================== 유사도/토큰 유틸 ====================

    private static final Pattern TOKEN = Pattern.compile("[\\p{IsHangul}\\p{L}\\p{Nd}]+");

    private static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        text = text.toLowerCase(Locale.ROOT);
        var m = TOKEN.matcher(text);
        Set<String> out = new LinkedHashSet<>();
        while (m.find()) {
            String tok = m.group().trim();
            if (tok.length() < 2) {
                continue;
            }
            out.add(tok);
        }
        return out;
    }

    private static double textSimilarity(ScoredDoc a, ScoredDoc b) {
        return jaccard(tokenize(a.content()), tokenize(b.content()));
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        int inter = 0;
        for (String t : a) {
            if (b.contains(t)) {
                inter++;
            }
        }
        int union = a.size() + b.size() - inter;
        if (union <= 0) {
            return 0.0;
        }
        return (double) inter / union;
    }
}
