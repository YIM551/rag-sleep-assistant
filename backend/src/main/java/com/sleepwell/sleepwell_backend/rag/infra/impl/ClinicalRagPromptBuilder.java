package com.sleepwell.sleepwell_backend.rag.infra.impl;

import com.sleepwell.sleepwell_backend.rag.infra.PromptBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 실제 서비스 지향 RAG 프롬프트 빌더:
 * - 시스템 지침에 안전/한계/근거 인용 명시
 * - 사용자 프로필(JSON) 반영
 * - CONTEXT 블록에 인용(제목/URL/발췌) 나열
 * - 출력 포맷(요약/근거/가이드/참고문헌) 고정
 */
@Component
@Primary
public class ClinicalRagPromptBuilder implements PromptBuilder {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String SYSTEM_PROMPT = """
      당신은 수면의학·건강 분야 RAG 어시스턴트입니다.
      반.드.시 다음 지침을 따르세요:
      1) 제공된 CONTEXT만을 근거로 답하세요. 컨텍스트에 없는 사실은 추정하지 마세요.
      2) 확실치 않으면 "자료상 확인 불가"라고 명시하고, 추가로 필요한 정보만 간단히 제안하세요.
      3) 의료적 조언은 일반 정보 차원임을 밝히고, 응급/중증은 전문의 상담을 권고하세요.
      4) 핵심 근거를 '참고문헌' 섹션에서 번호로 연결해 주세요.
      5) 한국어로 간결하고 구조적으로 응답하세요(마크다운 사용).

      출력 포맷(마크다운):
      ## 요약
      - 한두 문장 핵심 결론

      ## 핵심 근거
      - 주장 A — [1], [2]
      - 주장 B — [2]

      ## 상담 가이드(일반 정보)
      - 생활/행동 권고 (안전 주의 포함)
      - 전문의 상담이 필요한 경우

      ## 참고문헌
      [1] 제목 (URL)
      [2] 제목 (URL)
      """;

  @Override
  public Built build(String query, List<Cite> ctx, Map<String, Object> profile) {
    StringBuilder user = new StringBuilder();

    // 프로필(있을 때만)
    if (profile != null && !profile.isEmpty()) {
      user.append("사용자 프로필(JSON): ")
          .append(toJson(profile))
          .append("\n\n");
    }

    // 사용자 질문
    user.append("질문: ").append(nvl(query)).append("\n\n");

    // CONTEXT 블록
    user.append("CONTEXT 시작\n");
    if (ctx == null || ctx.isEmpty()) {
      user.append("(컨텍스트 없음)\n");
    } else {
      StringJoiner sj = new StringJoiner("\n---\n", "", "\n");
      int i = 1;
      for (Cite c : ctx) {
        String title = nvl(c.title());
        String url = nvl(c.url());
        String quote = clip(nvl(c.quote()), 1500);
        String block = "[" + i + "] " + (title.isBlank() ? "제목없음" : title)
            + (url.isBlank() ? "" : " (" + url + ")")
            + "\n" + quote;
        sj.add(block);
        i++;
      }
      user.append(sj).append("\n");
    }
    user.append("CONTEXT 끝\n\n");

    user.append("""
        요청:
        - 위 CONTEXT만 근거로, 지정한 출력 포맷(요약/핵심 근거/상담 가이드/참고문헌)으로 한국어 답변을 작성하세요.
        - 각 주장 옆에는 [번호]로 근거를 연결하고, '참고문헌' 섹션에 같은 번호로 제목/URL을 나열하세요.
        - 컨텍스트에 없으면 '자료상 확인 불가'라고 명시하세요.
        """);

    return new Built(SYSTEM_PROMPT, user.toString(), ctx);
  }

  // --------- helpers ---------
  private static String nvl(String s) {
    return s == null ? "" : s;
  }

  private static String clip(String s, int max) {
    String t = s.strip();
    return (t.length() > max) ? t.substring(0, max) + "…" : t;
  }

  private static String toJson(Map<String, Object> m) {
    try {
      return MAPPER.writeValueAsString(m);
    } catch (Exception e) {
      return String.valueOf(m);
    }
  }
}
