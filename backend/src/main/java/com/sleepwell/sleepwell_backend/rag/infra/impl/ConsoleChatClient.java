package com.sleepwell.sleepwell_backend.rag.infra.impl;

import com.sleepwell.sleepwell_backend.rag.infra.RagChatClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 데모/로컬 테스트용 RAG ChatClient.
 * - 실제 LLM 호출 없이 사용자의 프롬프트 일부를 흉내 내서 스트리밍합니다.
 * - 프로파일 "console"일 때만 활성화됩니다. (SpringAiChatClient와 빈 충돌 방지)
 */
@Component
@Profile("console")
public class ConsoleChatClient implements RagChatClient {

  /** 한 번에 흘려보낼 토큰 사이 대기(ms) */
  private static final long STREAM_DELAY_MS = 15L;
  /** 전체 응답 최대 길이 (데모용 안전장치) */
  private static final int MAX_ECHO_LEN = 600;

  @Override
  public String chat(String system, String user) {
    // 시스템 프롬프트는 무시하고 사용자 질문을 잘라서 가짜 응답 생성
    String body = user == null ? "" : user.strip();
    if (body.length() > MAX_ECHO_LEN) {
      body = body.substring(0, MAX_ECHO_LEN) + "…";
    }
    return "[DEMO STREAM] " + body;
  }

  @Override
  public void chatStream(String system,
      String user,
      Consumer<String> onDelta,
      Runnable onDone) {
    String msg = chat(system, user);

    // 단어 단위로 흘려보내기
    String[] tokens = msg.split("\\s+");
    AtomicBoolean cancelled = new AtomicBoolean(false);

    try {
      for (int i = 0; i < tokens.length; i++) {
        // 외부에서 인터럽트(연결 종료 등) 되었는지 체크
        if (Thread.currentThread().isInterrupted()) {
          cancelled.set(true);
          break;
        }
        // delta 전송
        onDelta.accept(tokens[i] + (i < tokens.length - 1 ? " " : ""));
        try {
          Thread.sleep(STREAM_DELAY_MS);
        } catch (InterruptedException ie) {
          // 인터럽트 신호 복구 후 종료
          Thread.currentThread().interrupt();
          cancelled.set(true);
          break;
        }
      }
    } catch (Exception ignored) {
      // onDelta에서 발생하는 예외(연결 끊김 등)는 조용히 무시
      cancelled.set(true);
    } finally {
      // 스트림 종료 알림 (연결이 이미 닫혔으면 호출되어도 무해)
      try {
        onDone.run();
      } catch (Exception ignored) {
      }
    }
  }
}
