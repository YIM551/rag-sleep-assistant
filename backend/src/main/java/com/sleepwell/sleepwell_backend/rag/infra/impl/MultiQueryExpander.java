package com.sleepwell.sleepwell_backend.rag.infra.impl;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 가벼운 사전/룰 기반 질의 확장기.
 * - 입력 질의의 의도를 감지해 한/영 동의어와 조합 질의를 만들어 반환한다.
 * - 원문 질의는 반환하지 않는다(서비스 레이어에서 base를 이미 추가함).
 * - "침/전침" 의도가 있으면 RCT/메타분석/PSQI 등 근거 중심 키워드를 주입한다.
 */
@Component
public class MultiQueryExpander {

    /** 단어경계 감지용(영문 약어) */
    private static final Pattern EA_WORD = Pattern.compile("\\bea\\b"); // electroacupuncture 약어
    private static final Pattern CBTI_WORD = Pattern.compile("\\bcbt-?i\\b"); // CBT-I / CBTI

    /** 핵심 용어 동의어(한↔영 포함) */
    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
            // 불면/수면
            Map.entry("불면", List.of("insomnia", "sleep disturbance", "sleep quality")),
            Map.entry("불면증", List.of("insomnia", "sleep disorder", "sleep quality")),
            Map.entry("insomnia", List.of("sleep disturbance", "sleep quality", "sleep disorder")),
            // 폐경
            Map.entry("폐경", List.of("menopause", "menopausal", "climacteric")),
            Map.entry("폐경기", List.of("menopause", "menopausal", "climacteric")),
            Map.entry("갱년기", List.of("menopause", "menopausal", "climacteric")),
            Map.entry("menopause", List.of("menopausal", "climacteric")),
            // 침 치료
            Map.entry("침", List.of("침술", "acupuncture")),
            Map.entry("침술", List.of("acupuncture")),
            Map.entry("전침", List.of("electroacupuncture", "EA")),
            Map.entry("acupuncture", List.of("electroacupuncture", "EA")),
            // 코골이/수면무호흡
            Map.entry("코골이", List.of("snoring", "snore reduction")),
            Map.entry("수면무호흡", List.of("sleep apnea", "obstructive sleep apnea", "OSA")),
            Map.entry("snoring", List.of("snore reduction")),
            Map.entry("sleep apnea", List.of("obstructive sleep apnea", "OSA")));

    /** 근거/시험/지표 키워드 */
    private static final List<String> RCT_TERMS = List.of(
            "RCT", "randomized controlled trial", "무작위대조시험", "sham-controlled");
    private static final List<String> EVIDENCE_TERMS = List.of(
            "systematic review", "meta-analysis", "clinical guideline",
            "체계적 문헌고찰", "메타분석", "진료지침");
    private static final List<String> METRIC_TERMS = List.of(
            "PSQI", "Pittsburgh Sleep Quality Index",
            "ISI", "Insomnia Severity Index",
            "ESS", "Epworth Sleepiness Scale");

    public List<String> expand(String query) {
        if (query == null || query.isBlank())
            return List.of();

        final String q = query.trim();
        final String lower = q.toLowerCase(Locale.ROOT);

        final boolean hasMenopause = containsAny(q, "폐경", "폐경기", "갱년기")
                || containsAny(lower, "menopaus", "climacteric");
        final boolean hasInsomnia = containsAny(q, "불면", "불면증") || containsAny(lower, "insomnia", "sleep disturbance");
        final boolean hasAcu = containsAny(q, "침", "침술", "전침")
                || containsAny(lower, "acupuncture", "electroacupuncture")
                || EA_WORD.matcher(lower).find();
        final boolean hasOSA = containsAny(q, "코골이", "수면무호흡")
                || containsAny(lower, "sleep apnea", "obstructive sleep apnea", "snor", "osa");
        final boolean wantsEvidence = containsAny(q, "근거", "지침", "문헌고찰", "메타분석")
                || containsAny(lower, "evidence", "rct", "randomized", "meta", "systematic review", "guideline")
                || containsAny(lower, "psqi", "isi", "epworth");

        LinkedHashSet<String> out = new LinkedHashSet<>();

        // 1) 사전 기반 동의어 (질의에 등장한 key의 동의어만 추가)
        for (var e : SYNONYMS.entrySet()) {
            String key = e.getKey();
            if (q.contains(key) || lower.contains(key.toLowerCase(Locale.ROOT))) {
                e.getValue().forEach(s -> safeAdd(out, s));
            }
        }

        // 2) 조합 규칙 — 의도별 주입
        if (hasMenopause && hasInsomnia) {
            safeAdd(out, "menopausal insomnia");
            safeAdd(out, "climacteric insomnia");

            if (hasAcu) {
                // 침/전침이 들어간 경우: 근거/지표 키워드 주입
                safeAdd(out, "acupuncture menopausal insomnia");
                safeAdd(out, "electroacupuncture menopausal insomnia");
                safeAdd(out, "acupuncture insomnia PSQI");
                safeAdd(out, "electroacupuncture sleep quality");
                safeAdd(out, "갱년기 불면 전침 PSQI");

                for (String t : RCT_TERMS) {
                    safeAdd(out, "acupuncture menopausal insomnia " + t);
                    safeAdd(out, "electroacupuncture menopausal insomnia " + t);
                }
                METRIC_TERMS.forEach(m -> safeAdd(out, "acupuncture insomnia " + m));
                // CBT-I 비교 검색도 함께
                if (!CBTI_WORD.matcher(lower).find()) {
                    safeAdd(out, "CBT-I menopause insomnia RCT");
                    safeAdd(out, "CBTI menopause insomnia");
                }
            } else {
                safeAdd(out, "menopausal insomnia treatment options");
                safeAdd(out, "CBT-I menopause insomnia RCT");
                safeAdd(out, "hormone therapy menopause insomnia");
            }
        }

        if (hasAcu && hasInsomnia && !hasMenopause) {
            safeAdd(out, "acupuncture for insomnia");
            safeAdd(out, "electroacupuncture sleep quality");
            safeAdd(out, "acupuncture insomnia PSQI ISI");
            for (String t : RCT_TERMS)
                safeAdd(out, "acupuncture insomnia " + t);
            METRIC_TERMS.forEach(m -> safeAdd(out, "electroacupuncture insomnia " + m));
        }

        if (hasAcu && hasMenopause && !hasInsomnia) {
            safeAdd(out, "acupuncture menopause sleep quality");
            safeAdd(out, "electroacupuncture menopause hot flashes sleep");
            for (String t : RCT_TERMS)
                safeAdd(out, "acupuncture menopause " + t);
        }

        if (hasOSA) {
            safeAdd(out, "CPAP efficacy");
            safeAdd(out, "AHI reduction CPAP");
            safeAdd(out, "mandibular advancement device snoring");
            for (String t : RCT_TERMS)
                safeAdd(out, "snoring OSA " + t);
        }

        // 3) '근거/evidence' 요구 시, 근거 중심 확장 추가 (침 의도가 있으면 침과 결합)
        if (wantsEvidence) {
            EVIDENCE_TERMS.forEach(t -> safeAdd(out, t));
            if (hasAcu && (hasMenopause || hasInsomnia)) {
                safeAdd(out, "acupuncture menopausal insomnia meta-analysis");
                safeAdd(out, "acupuncture insomnia systematic review");
                safeAdd(out, "electroacupuncture insomnia randomized controlled trial");
                safeAdd(out, "전침 불면 메타분석");
                safeAdd(out, "침 치료 폐경기 불면 체계적 문헌고찰");
            }
        }

        // 4) 정리: 원문 질의 제거, 공백/중복 제거, 길이 제한(서비스에서 추가 컷)
        out.removeIf(s -> s == null || s.isBlank() || s.equalsIgnoreCase(q));

        List<String> result = new ArrayList<>(out);
        if (result.size() > 12)
            result = result.subList(0, 12); // 넉넉히 생성 → 서비스 레이어에서 6개로 컷
        return result;
    }

    /* ------------------------ utils ------------------------ */

    private static boolean containsAny(String hay, String... needles) {
        if (hay == null)
            return false;
        for (String n : needles) {
            if (n == null)
                continue;
            if (hay.contains(n))
                return true;
        }
        return false;
    }

    private static void safeAdd(Set<String> set, String s) {
        if (s == null)
            return;
        String t = s.trim();
        if (!t.isBlank())
            set.add(t);
    }
}
