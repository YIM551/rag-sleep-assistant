package com.sleepwell.sleepwell_backend.rag.service;

import com.sleepwell.sleepwell_backend.rag.infra.impl.MultiQueryExpander;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleepwell.sleepwell_backend.rag.dto.RagQueryRequest;
import com.sleepwell.sleepwell_backend.rag.infra.PromptBuilder;
import com.sleepwell.sleepwell_backend.rag.infra.RagChatClient;
import com.sleepwell.sleepwell_backend.rag.infra.impl.GuardrailService;
import com.sleepwell.sleepwell_backend.rag.infra.impl.HybridRetriever;
import com.sleepwell.sleepwell_backend.rag.infra.port.RerankerPort;
import com.sleepwell.sleepwell_backend.rag.model.ScoredDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class RagQueryService {

    private static final Logger log = LoggerFactory.getLogger(RagQueryService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 간단 영/한 스톱워드(필요시 확장)
    private static final Set<String> STOP_EN = Set.of(
            "a", "an", "the", "and", "or", "for", "of", "to", "in", "on", "at", "by", "from", "with",
            "is", "are", "was", "were", "be", "been", "being", "as", "that", "this", "these", "those",
            "it", "its", "into", "about", "over", "under", "between", "among", "within", "without",
            "not", "no", "yes", "if", "then", "else", "than", "such", "also", "may", "might", "can", "could",
            "should", "would", "will", "do", "does", "did", "done", "have", "has", "had", "having",
            "we", "you", "they", "he", "she", "i", "me", "my", "our", "their", "his", "her", "them");

    private static final Set<String> STOP_KO = Set.of(
            "그리고", "또한", "또", "및", "등", "또는", "하지만", "그러나", "때문", "때문에", "대한", "대해",
            "에", "에서", "으로", "를", "을", "이", "가", "은", "는", "와", "과", "도", "만", "보다",
            "수", "등의", "등을", "있는", "없는", "하는", "했다", "합니다", "한다", "하여", "해서", "하며",
            "것", "등등", "혹은", "즉", "등지", "위해", "관련", "관련한", "관련하여", "중", "등에");

    private static final Pattern TOKEN = Pattern.compile("[\\p{IsHangul}\\p{L}\\p{Nd}]+");
    private static final Pattern PCT_ENC_PATTERN = Pattern.compile("%[0-9a-fA-F]{2}");

    private final PromptBuilder prompt;
    private final RagChatClient chat;
    private final HybridRetriever hybrid;
    private final MultiQueryExpander expander;
    private final Optional<RerankerPort> reranker; // 옵션
    private final Optional<GuardrailService> guard; // 옵션

    @Value("${rag.retrieval.dense.topKInit:60}")
    private int denseTopK;
    @Value("${rag.retrieval.sparse.topKInit:60}")
    private int sparseTopK;
    @Value("${rag.retrieval.mmr.enabled:true}")
    private boolean useMmr;
    @Value("${rag.retrieval.mmr.k:25}")
    private int mmrK;
    @Value("${rag.retrieval.mmr.lambda:0.45}")
    private double mmrLambda;
    @Value("${rag.retrieval.rerank.enabled:true}")
    private boolean useRerank;
    @Value("${rag.retrieval.rerank.topK:20}")
    private int rerankTopK;

    public RagQueryService(
            PromptBuilder prompt,
            RagChatClient chat,
            HybridRetriever hybrid,
            MultiQueryExpander expander,
            Optional<RerankerPort> reranker,
            Optional<GuardrailService> guard) {
        this.prompt = prompt;
        this.chat = chat;
        this.hybrid = hybrid;
        this.expander = expander;
        this.reranker = reranker;
        this.guard = guard;
    }

    /** 동기 응답: 간단보기/자세히보기로 분리 */
    public Map<String, Object> answer(RagQueryRequest req) throws Exception {
        long totalStart = System.nanoTime();
        Map<String, Double> timings = new LinkedHashMap<>();
        // 2026 portfolio fix: reject invalid input before guard/retrieval/model calls.
        if (req == null || req.query() == null || req.query().isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        if (req.query().length() > 4000) {
            throw new IllegalArgumentException("query must contain at most 4000 characters");
        }
        if (req.topK() != null && (req.topK() < 1 || req.topK() > 50)) {
            throw new IllegalArgumentException("topK must be between 1 and 50");
        }
        if (guard.isPresent() && guard.get().hasRedFlag(req.query())) {
            String msg = "⚠️ 위급 징후 가능성이 있는 표현이 포함되어 있습니다. 즉시 전문의 상담 또는 응급실 방문을 권고합니다.";
            return Map.of(
                    "message", "### 간단보기\n" + msg + "\n\n### 자세히보기\n" + msg,
                    "message_compact", msg,
                    "message_detailed", msg,
                    "citations", List.of());
        }

        // 검색 & 인용 필터링
        var ctx = retrieveWithFilters(req, timings);

        // 본문 생성(자세히보기)
        long promptStart = System.nanoTime();
        var built = prompt.build(req.query(), ctx, profileMap(req));
        timings.put("prompt_build_ms", elapsedMs(promptStart));
        long llmStart = System.nanoTime();
        String detailed = chat.chat(built.system(), built.user());
        timings.put("llm_ms", elapsedMs(llmStart));

        // 간단보기: LLM 재호출 없이 첫 2문장만 잘라서 사용
        String compact = extractFirstSentences(detailed, 2);
        timings.put("total_ms", elapsedMs(totalStart));
        // 2026 instrumentation: per-request successful-path durations only; no query/profile text.
        log.info("RAG_STAGE_TIMING {}", timings);

        return Map.of(
                "message", renderCombined(compact, detailed),
                "message_compact", nullToEmpty(compact),
                "message_detailed", nullToEmpty(detailed),
                "citations", ctx);
    }

    // -------------------- 검색/재랭킹 + 필터 --------------------

    private List<PromptBuilder.Cite> retrieveWithFilters(RagQueryRequest req, Map<String, Double> timings) throws Exception {
        int topK = Optional.ofNullable(req.topK()).orElse(5);
        // 네임스페이스 미지정 시 null (전체 검색)
        String ns = Optional.ofNullable(req.namespace()).filter(s -> !s.isBlank()).orElse(null);

        // ✅ 방어적 URL 디코딩(최대 2회)으로 이중 인코딩 입력을 복원
        String queryRaw = Optional.ofNullable(req.query()).orElse("").strip();
        String query = urlDecodeDefensively(queryRaw);
        if (query.isEmpty()) {
            throw new IllegalArgumentException("query must not be blank");
        }

        // 질의 확장 (우선순위 정렬 포함) - 성능 최적화: 6개 → 3개
        long expansionStart = System.nanoTime();
        List<String> expansions = expandQueries(query, topK);
        if (expansions.size() > 3)
            expansions = expansions.subList(0, 3);
        log.debug("[RAG] expansions count={}", expansions.size());
        timings.put("query_expansion_ms", elapsedMs(expansionStart));

        // 하이브리드 검색 (RRF), 네임스페이스 필터링 (null이면 전체 검색)
        long retrievalStart = System.nanoTime();
        int perQueryK = Math.max(10, topK * 3);
        List<ScoredDoc> all = new ArrayList<>();
        for (String qx : expansions) {
            List<ScoredDoc> fused = hybrid.search(qx, denseTopK, sparseTopK, useMmr, mmrK, mmrLambda);
            fused.stream()
                    .filter(sd -> {
                        // 네임스페이스 미지정 시 전체 검색
                        if (ns == null)
                            return true;
                        Map<String, Object> m = sd.meta();
                        String docNs = String.valueOf(m == null ? "default" : m.getOrDefault("namespace", "default"));
                        return docNs.equals(ns);
                    })
                    .limit(perQueryK)
                    .forEach(all::add);
        }

        // 🔧 키워드 필터: 원문 + 확장쿼리에서 키워드 추출 → 본문/제목/파일명에 포함된 것만
        String kwSource = query + " " + String.join(" ", expansions);
        Set<String> qKeywords = extractKeywords(kwSource);
        log.debug("[RAG] keyword filter size={}", qKeywords.size());
        List<ScoredDoc> afterKw = all;
        if (!qKeywords.isEmpty()) {
            List<ScoredDoc> kwFiltered = all.stream()
                    .filter(sd -> matchesKeywords(sd, qKeywords))
                    .collect(toList());
            if (!kwFiltered.isEmpty())
                afterKw = kwFiltered; // 결과가 있으면 키워드 필터 적용, 아니면 폴백
        }

        // 중복 제거
        LinkedHashMap<String, ScoredDoc> uniq = new LinkedHashMap<>();
        for (ScoredDoc d : afterKw) {
            String key = Optional.ofNullable(d.id()).orElse("");
            if (key.isBlank()) {
                String filename = meta(d, "filename");
                String source = firstNonBlank(meta(d, "source"), meta(d, "url"), "");
                key = d.content().hashCode() + "|" + source + "|" + filename;
            }
            uniq.putIfAbsent(key, d);
        }
        List<ScoredDoc> deduped = new ArrayList<>(uniq.values());
        log.debug("[RAG] candidates after dedup: {}", deduped.size());
        timings.put("retrieval_ms", elapsedMs(retrievalStart));

        // ---------------- 재랭킹 (RRF → MMR diversity → 옵션 reranker) ----------------

        // 1차: 스코어 기준 정렬
        long rerankStart = System.nanoTime();
        List<ScoredDoc> sorted = new ArrayList<>(deduped);
        sorted.sort(Comparator.comparing(ScoredDoc::score).reversed());

        // 2차: MMR 기반 diversity 선택 (옵션)
        List<ScoredDoc> baseList = new ArrayList<>(sorted);
        if (useMmr && baseList.size() > 1) {
            int mmrLimit = Math.min(mmrK, baseList.size());
            baseList = applyMmrDiversity(baseList, mmrLimit, mmrLambda);
            log.debug("[RAG] MMR applied: selected={}/{}", baseList.size(), sorted.size());
        }

        // 3차: rerankTopK만큼 컷오프
        List<ScoredDoc> topCandidates = baseList.subList(0, Math.min(rerankTopK, baseList.size()));

        // 4차: 외부 reranker(예: RBF 모델)에 한 번 더 넘겨서 최종 topK 선택 (옵션)
        List<ScoredDoc> picked = (useRerank && reranker.isPresent())
                ? reranker.get().rerank(query, topCandidates, topK)
                : topCandidates.subList(0, Math.min(topK, topCandidates.size()));
        timings.put("reranking_ms", elapsedMs(rerankStart));

        // Cite 구성 + 동일 문서(title|url) 중복 제거 + URL sanitize
        LinkedHashMap<String, PromptBuilder.Cite> citeMap = new LinkedHashMap<>();
        for (ScoredDoc sd : picked) {
            String title = firstNonBlank(meta(sd, "filename"), meta(sd, "title"), "Reference");
            String rawUrl = firstNonBlank(meta(sd, "source"), meta(sd, "url"), "");
            String url = sanitizeUrl(rawUrl); // 로컬/파일 경로 제거

            String key = (title + "|" + url);
            citeMap.putIfAbsent(key, new PromptBuilder.Cite(
                    title,
                    url,
                    firstNonBlank(sd.metaString("snippet"), pickQuote(sd.content()))));
        }
        return new ArrayList<>(citeMap.values());
    }

    // -------------------- 질의 확장 --------------------

    /**
     * 쿼리 확장 (2026: private self-invocation 캐시 주석/annotation 제거)
     * - 기존 private @Cacheable 호출은 프록시를 거치지 않아 캐싱을 보장하지 않았음.
     * - 현재는 이 세 메서드에 캐시를 적용하지 않는다.
     */
    private List<String> expandQueries(String q, int topK) {
        log.debug("[RAG] query expansion input length={}", q == null ? 0 : q.length());

        LinkedHashSet<String> out = new LinkedHashSet<>();
        String base = Optional.ofNullable(q).orElse("").trim();
        if (base.isEmpty())
            return List.of(base);
        out.add(base);

        // A) 룰/사전 기반 멀티쿼리
        try {
            for (String s : expander.expand(base)) {
                String c = cleanVariant(s);
                if (!c.isBlank())
                    out.add(c);
            }
        } catch (Exception ignore) {
        }

        // B) LLM 간단 리라이트(실패해도 무시)
        try {
            List<String> llmVariants = getLlmQueryVariants(base);
            llmVariants.stream()
                    .map(this::cleanVariant)
                    .filter(s -> !s.isBlank())
                    .forEach(out::add);
        } catch (Exception ignore) {
        }

        // C) PRF (간단 버전)
        try {
            int prfK = Math.max(8, topK * 4);
            List<ScoredDoc> seeds = getPrfSeeds(base);
            String joined = seeds.stream().limit(prfK).map(ScoredDoc::content).collect(Collectors.joining(" "));
            List<String> keywords = topKeywords(joined, 12);
            if (!keywords.isEmpty()) {
                String boosted1 = base + " " + String.join(" ", keywords.subList(0, Math.min(6, keywords.size())));
                out.add(boosted1);
                if (keywords.size() > 6) {
                    String boosted2 = base + " " + String.join(" ", keywords.subList(6, Math.min(12, keywords.size())));
                    out.add(boosted2);
                }
            }
        } catch (Exception ignore) {
        }

        // ✅ 우선순위 정렬로 근거 중심/침 관련 쿼리를 앞으로
        List<String> prioritized = prioritizeExpansions(base, new ArrayList<>(out));

        // 길이 제한 및 상한
        prioritized = prioritized.stream()
                .map(s -> s.length() > 180 ? s.substring(0, 180) : s)
                .collect(toList());
        if (prioritized.size() > 12) {
            prioritized = prioritized.subList(0, 12);
        }
        return prioritized;
    }

    /**
     * LLM 쿼리 리라이트
     */
    private List<String> getLlmQueryVariants(String query) {
        log.debug("[RAG] query rewrite input length={}", query.length());
        try {
            String sys = "You are a query rewriting engine for retrieval. "
                    + "Return only a JSON array of 3-5 short diversified queries. "
                    + "Include paraphrases, common synonyms, acronyms, and cross-lingual variants (Korean/English) when helpful. "
                    + "Keep each under 12 words; no explanations.";
            String user = "Query: " + query;
            String json = chat.chat(sys, user);
            return tryParseStringArray(json);
        } catch (Exception e) {
            log.warn("[RAG] query rewrite failed: errorType={}", e.getClass().getSimpleName());
            return List.of();
        }
    }

    /**
     * PRF 시드 문서 검색
     */
    private List<ScoredDoc> getPrfSeeds(String query) {
        log.debug("[RAG] PRF input length={}", query.length());
        return hybrid.search(query, Math.min(20, denseTopK), Math.min(20, sparseTopK), false, 10, 0.4);
    }

    private List<String> tryParseStringArray(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            // JSON 배열이 아니면 줄단위로 폴백
            return Arrays.stream(json.split("\\r?\\n"))
                    .map(s -> s.replaceAll("^[-*\\s]+", "")) // bullet 제거
                    .filter(s -> !s.isBlank())
                    .collect(toList());
        }
    }

    /** 리라이트 문자열 정리(따옴표/브라켓/코드펜스/말미 구두점 등 제거) */
    private String cleanVariant(String s) {
        if (s == null)
            return "";
        String t = s.trim();

        // 코드펜스 전체 제거(멀티라인)
        t = t.replaceAll("(?s)^```.*?```\\s*$", "").trim();

        // 외곽 브라켓/괄호/여분 공백 제거
        t = t.replaceAll("^[\\[\\(\\{\\s]+", "")
                .replaceAll("[\\]\\)\\}\\s]+$", "")
                .trim();

        // 스마트/일반 따옴표/백틱 제거(양끝)
        t = t.replaceAll("^[\"“”‘’`]+", "")
                .replaceAll("[\"“”‘’`]+$", "")
                .trim();

        // 말미 구두점 전반 제거(루씬 파서 안정성)
        t = t.replaceAll("[\\p{Punct}\\p{IsPunctuation}]+$", "").trim();

        // 중복 공백 정리
        t = t.replaceAll("\\s{2,}", " ").trim();

        return t;
    }

    private static List<String> topKeywords(String text, int limit) {
        if (text == null || text.isBlank())
            return List.of();
        Map<String, Integer> freq = new HashMap<>();
        var m = TOKEN.matcher(text);
        while (m.find()) {
            String tok = m.group().toLowerCase(Locale.ROOT);
            if (tok.length() < 2)
                continue;
            if (STOP_EN.contains(tok))
                continue;
            if (STOP_KO.contains(tok))
                continue;
            freq.merge(tok, 1, Integer::sum);
        }
        return freq.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(toList());
    }

    // -------------------- 키워드 필터 --------------------

    private Set<String> extractKeywords(String q) {
        if (q == null)
            return Set.of();
        LinkedHashSet<String> kws = new LinkedHashSet<>();
        var m = TOKEN.matcher(q);
        while (m.find()) {
            String tok = m.group().toLowerCase(Locale.ROOT);
            if (tok.length() < 2)
                continue;
            if (STOP_EN.contains(tok))
                continue;
            if (STOP_KO.contains(tok))
                continue;
            kws.add(tok);
        }
        return kws;
    }

    private boolean matchesKeywords(ScoredDoc d, Set<String> kws) {
        if (kws.isEmpty())
            return true;
        String title = meta(d, "title");
        String filename = meta(d, "filename");
        String hay = (nullToEmpty(d.content()) + " " + title + " " + filename).toLowerCase(Locale.ROOT);
        for (String k : kws) {
            if (hay.contains(k))
                return true;
        }
        return false;
    }

    // -------------------- 유틸 --------------------

    private static double elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000.0;
    }

    /**
     * 텍스트에서 첫 N개 문장 추출 (간단보기용)
     * - LLM을 추가 호출하지 않고 첫 문장들을 추출
     */
    private String extractFirstSentences(String text, int count) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // 한글/영문 문장 종결 기준: ., !, ?, 。
        String[] sentences = text.split("[.!?。]\\s+");

        StringBuilder result = new StringBuilder();
        int limit = Math.min(count, sentences.length);
        for (int i = 0; i < limit; i++) {
            result.append(sentences[i].trim());
            if (i < limit - 1) {
                result.append(". ");
            }
        }

        return result.toString();
    }

    private String renderCombined(String compact, String detailed) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 간단보기\n").append(nullToEmpty(compact)).append("\n\n");
        sb.append("### 자세히보기\n").append(nullToEmpty(detailed));
        return sb.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // 인용문: PDF 추출 텍스트 가독성 보정 + 280자 컷
    private String pickQuote(String text) {
        if (text == null)
            return "";
        String t = text
                .replaceAll("-\\s*\\n", "")
                .replaceAll("-\\s+", "")
                .replaceAll("\\s*\\n\\s*", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return t.length() > 280 ? t.substring(0, 280) + "…" : t;
    }

    private Map<String, Object> profileMap(RagQueryRequest req) {
        var p = req.profile();
        var m = new HashMap<String, Object>();
        if (p != null) {
            m.put("name", p.name());
            m.put("age", p.age());
            m.put("sex", p.sex());
            m.put("occupation", p.occupation());
            if (p.extras() != null)
                m.putAll(p.extras());
        }
        return m;
    }

    private static String meta(ScoredDoc d, String key) {
        Map<String, Object> m = d.meta();
        if (m == null)
            return "";
        Object v = m.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private static String firstNonBlank(String... vals) {
        for (String s : vals)
            if (s != null && !s.isBlank())
                return s;
        return "";
    }

    /** URL 이중 인코딩 방어: 최대 2회까지 decode, decode로 값이 변하면 한 번 더 시도 */
    private static String urlDecodeDefensively(String s) {
        if (s == null || s.isBlank())
            return "";
        String prev = s;
        for (int i = 0; i < 2; i++) {
            try {
                String decoded = URLDecoder.decode(prev, StandardCharsets.UTF_8);
                if (decoded.equals(prev)) {
                    // 더 이상 변화 없음. 혹시 여전히 %XX가 남아있으면 한 번 더 시도 후 종료
                    if (PCT_ENC_PATTERN.matcher(decoded).find()) {
                        prev = decoded;
                        continue;
                    }
                    return decoded;
                } else {
                    prev = decoded;
                }
            } catch (IllegalArgumentException e) {
                // 잘못된 인코딩이면 원문 반환
                return prev;
            }
        }
        return prev;
    }

    // -------------------- 확장 쿼리 우선순위 정렬 --------------------

    /** 침/전침, 근거(RCT/메타/PSQI 등) 포함 쿼리를 상위에 오도록 정렬하고 base는 항상 0번 인덱스. */
    private List<String> prioritizeExpansions(String base, List<String> items) {
        if (items == null || items.isEmpty())
            return List.of(base);

        // 정리
        items.removeIf(Objects::isNull);
        items.replaceAll(String::trim);
        items.removeIf(String::isBlank);

        // base는 맨 앞으로 고정
        items.remove(base);

        Comparator<String> cmp = (a, b) -> {
            int sa = score(a);
            int sb = score(b);
            if (sa != sb)
                return Integer.compare(sb, sa); // 높은 점수 우선
            // 동점이면 짧은 쿼리 우선(잡단어/군더더기 억제)
            int len = Integer.compare(a.length(), b.length());
            if (len != 0)
                return len;
            return a.compareToIgnoreCase(b);
        };

        // 중복 제거 후 정렬
        List<String> uniq = new ArrayList<>(new LinkedHashSet<>(items));
        uniq.sort(cmp);

        List<String> out = new ArrayList<>();
        out.add(base);
        out.addAll(uniq);
        return out;
    }

    /** 간단 스코어러: 침/전침 > 폐경·불면 조합 > 근거(RCT/메타/지표) > CBT */
    private int score(String s) {
        if (s == null || s.isBlank())
            return 0;
        String t = s.toLowerCase(Locale.ROOT);

        String[] hiAcu = { "acupuncture", "electroacupuncture", "전침", "침술", "침 " };
        String[] hiMenIns = { "menopausal insomnia", "갱년기 불면", "폐경기 불면" };
        String[] hiEvidence = { "rct", "randomized controlled", "무작위대조", "systematic review", "meta-analysis", "메타분석",
                "체계적 문헌고찰" };
        String[] hiMetrics = { "psqi", "isi" };
        String[] hiCBT = { "cbt-i", "cbt i", "cbt" };

        int sc = 0;
        for (String k : hiAcu)
            if (t.contains(k))
                sc += 5;
        for (String k : hiMenIns)
            if (t.contains(k))
                sc += 4;
        for (String k : hiEvidence)
            if (t.contains(k))
                sc += 3;
        for (String k : hiMetrics)
            if (t.contains(k))
                sc += 3;
        for (String k : hiCBT)
            if (t.contains(k))
                sc += 2;
        return sc;
    }

    /**
     * 간단한 MMR(Maximal Marginal Relevance) 기반 diversity 선택
     * - relevance: ScoredDoc.score() 사용
     * - redundancy: 제목/파일명/본문에서 토큰 셋을 만들어 Jaccard 유사도로 근사
     */
    private List<ScoredDoc> applyMmrDiversity(List<ScoredDoc> docs, int k, double lambda) {
        if (docs == null || docs.isEmpty() || k <= 0) {
            return List.of();
        }
        List<ScoredDoc> selected = new ArrayList<>();
        List<Set<String>> tokenSets = docs.stream()
                .map(this::tokensForMmr)
                .collect(toList());

        // 첫 번째 문서는 가장 relevance 높은 문서 (이미 score 기준 정렬되어 있음)
        selected.add(docs.get(0));

        while (selected.size() < Math.min(k, docs.size())) {
            double bestScore = Double.NEGATIVE_INFINITY;
            int bestIdx = -1;

            for (int i = 0; i < docs.size(); i++) {
                ScoredDoc candidate = docs.get(i);
                if (selected.contains(candidate)) {
                    continue;
                }

                double relevance = candidate.score(); // ScoringUtils.normalize로 0~1 근처라고 가정
                double redundancy = 0.0;

                Set<String> candTokens = tokenSets.get(i);
                for (ScoredDoc sel : selected) {
                    int selIdx = docs.indexOf(sel);
                    if (selIdx < 0)
                        continue;
                    Set<String> selTokens = tokenSets.get(selIdx);
                    redundancy = Math.max(redundancy, jaccardSimilarity(candTokens, selTokens));
                }

                double mmrScore = lambda * relevance - (1.0 - lambda) * redundancy;

                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    bestIdx = i;
                }
            }

            if (bestIdx < 0) {
                break;
            }
            selected.add(docs.get(bestIdx));
        }

        return selected;
    }

    /** MMR용 토큰 추출: 제목/파일명/본문 일부를 합쳐서 간단히 분해 */
    private Set<String> tokensForMmr(ScoredDoc d) {
        String title = meta(d, "title");
        String filename = meta(d, "filename");
        String text = (nullToEmpty(d.content()) + " " + title + " " + filename)
                .toLowerCase(Locale.ROOT);

        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        var m = TOKEN.matcher(text);
        while (m.find()) {
            String tok = m.group().toLowerCase(Locale.ROOT);
            if (tok.length() < 2)
                continue;
            if (STOP_EN.contains(tok))
                continue;
            if (STOP_KO.contains(tok))
                continue;
            tokens.add(tok);
        }
        return tokens;
    }

    /** Jaccard 유사도: |A∩B| / |A∪B| */
    private double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        if (inter.isEmpty())
            return 0.0;

        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }

    /** file://, 로컬 경로 등 비-웹 URL은 숨김 */
    private static String sanitizeUrl(String url) {
        if (url == null || url.isBlank())
            return "";
        String u = url.trim();

        // 로컬 파일/경로 형태는 숨김
        if (u.startsWith("file:") || u.startsWith("C:\\") || u.startsWith("/") || u.startsWith("\\")) {
            return "";
        }
        // http(s) 또는 DOI만 허용
        if (u.startsWith("http://") || u.startsWith("https://") || u.startsWith("doi:")) {
            return u;
        }
        return "";
    }
}
