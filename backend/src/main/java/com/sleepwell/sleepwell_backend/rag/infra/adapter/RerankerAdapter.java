package com.sleepwell.sleepwell_backend.rag.infra.adapter;

import com.sleepwell.sleepwell_backend.rag.infra.port.RerankerPort;
import com.sleepwell.sleepwell_backend.rag.model.ScoredDoc;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 고급 리랭킹 어댑터 (RBF 스타일 리랭커)
 *
 * - 기본 retrieval 점수(embedding/BM25 등)를 그대로 쓰지 않고,
 * 1) 쿼리-문서 키워드 겹침 정도
 * 2) 근거 키워드(RCT, 메타분석, PSQI/ISI, CBT-I 등) 포함 여부
 * 를 추가로 반영해서 점수를 다시 계산합니다.
 *
 * - 교수님 면담에서 나온 "RBF 리랭킹" 포인트를 반영한 단계:
 * * base score + lexical overlap + evidence boost 를 조합한
 * Relevance Boost Fusion(RBF) 스타일의 재랭킹입니다.
 *
 * - 향후 진짜 ML 기반 리랭커(OpenAI rerank, 전용 RBF 모델 등)로 교체해도
 * 이 어댑터만 바꾸면 되도록 인터페이스(RerankerPort)는 그대로 유지합니다.
 */
@Component
public class RerankerAdapter implements RerankerPort {

    private static final Pattern TOKEN = Pattern.compile("[\\p{IsHangul}\\p{L}\\p{Nd}]+");

    // 근거 강화 키워드들 (논문/임상 근거 중심)
    private static final String[] EVIDENCE_KEYWORDS = {
            "rct", "randomized controlled", "무작위대조",
            "systematic review", "체계적 문헌고찰",
            "meta-analysis", "메타분석",
            "psqi", "isi",
            "cbt-i", "cbt i", "cbt", "인지행동치료"
    };

    // 침/전침, 갱년기 불면 같은 도메인 핵심 키워드 (조금 더 가중치)
    private static final String[] DOMAIN_KEYWORDS = {
            "acupuncture", "electroacupuncture", "전침", "침술", "침 ",
            "menopausal insomnia", "갱년기 불면", "폐경기 불면"
    };

    @Override
    public List<ScoredDoc> rerank(String query, List<ScoredDoc> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        String q = Optional.ofNullable(query).orElse("").toLowerCase(Locale.ROOT);
        Set<String> qTokens = tokenize(q);

        List<ScoredDoc> rescored = new ArrayList<>(candidates.size());

        for (ScoredDoc doc : candidates) {
            double baseScore = Optional.ofNullable(doc.score()).orElse(0.0);

            // 문서 텍스트: 본문 + title/filename/snippet 등 메타까지 합침
            String docText = buildDocText(doc).toLowerCase(Locale.ROOT);

            // 1) 쿼리-문서 키워드 겹침 정도 (0 ~ 1 근사)
            double overlapScore = lexicalOverlap(qTokens, docText);

            // 2) 근거 키워드 / 도메인 키워드 보너스
            double evidenceScore = evidenceBoost(docText);
            double domainScore = domainBoost(docText);

            /**
             * 최종 점수 계산 (가중치는 필요하면 yml로 빼서 조정 가능):
             * - baseScore: 0.7
             * - lexical overlap: 0.2
             * - evidence boost + domain boost: 0.1
             */
            double finalScore = 0.7 * baseScore
                    + 0.2 * overlapScore
                    + 0.1 * (0.6 * evidenceScore + 0.4 * domainScore);

            rescored.add(doc.withScore(finalScore));
        }

        // 최종 점수 기준 내림차순 정렬
        rescored.sort(Comparator.comparing(ScoredDoc::score).reversed());

        return rescored.subList(0, Math.min(topK, rescored.size()));
    }

    // -------------------- 내부 유틸 --------------------

    /** 쿼리/문서 텍스트를 토큰 세트로 변환 */
    private static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        var m = TOKEN.matcher(text);
        while (m.find()) {
            String tok = m.group().toLowerCase(Locale.ROOT).trim();
            if (tok.length() < 2)
                continue;
            out.add(tok);
        }
        return out;
    }

    /** ScoredDoc에서 본문 + 메타 정보(title, filename, snippet 등)를 묶어서 하나의 텍스트로 만든다. */
    private static String buildDocText(ScoredDoc doc) {
        StringBuilder sb = new StringBuilder();
        if (doc.content() != null) {
            sb.append(doc.content()).append(' ');
        }
        Map<String, Object> meta = doc.meta();
        if (meta != null && !meta.isEmpty()) {
            appendIfPresent(sb, meta, "title");
            appendIfPresent(sb, meta, "filename");
            appendIfPresent(sb, meta, "snippet");
            appendIfPresent(sb, meta, "summary");
        }
        return sb.toString();
    }

    private static void appendIfPresent(StringBuilder sb, Map<String, Object> meta, String key) {
        Object v = meta.get(key);
        if (v != null) {
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) {
                sb.append(s).append(' ');
            }
        }
    }

    /**
     * 간단한 lexical overlap:
     * - 쿼리 토큰 중 문서에 등장하는 토큰 비율 (0.0 ~ 1.0 근사)
     */
    private static double lexicalOverlap(Set<String> qTokens, String docText) {
        if (qTokens.isEmpty() || docText == null || docText.isBlank()) {
            return 0.0;
        }
        Set<String> docTokens = tokenize(docText);
        if (docTokens.isEmpty()) {
            return 0.0;
        }

        int match = 0;
        for (String q : qTokens) {
            if (docTokens.contains(q)) {
                match++;
            }
        }
        return (double) match / (double) qTokens.size();
    }

    /** 근거(Evidence) 키워드 포함 정도를 0 ~ 1 사이로 정규화한 점수 */
    private static double evidenceBoost(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        String t = text.toLowerCase(Locale.ROOT);
        int hits = 0;
        for (String k : EVIDENCE_KEYWORDS) {
            if (t.contains(k.toLowerCase(Locale.ROOT))) {
                hits++;
            }
        }
        // 0 ~ 1 사이로 스케일링 (3개 이상이면 최대치로 캡)
        return Math.min(1.0, hits / 3.0);
    }

    /** 침/전침, 갱년기 불면 등 도메인 특화 키워드 boost (0 ~ 1) */
    private static double domainBoost(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        String t = text.toLowerCase(Locale.ROOT);
        int hits = 0;
        for (String k : DOMAIN_KEYWORDS) {
            if (t.contains(k.toLowerCase(Locale.ROOT))) {
                hits++;
            }
        }
        return Math.min(1.0, hits / 2.0);
    }
}
