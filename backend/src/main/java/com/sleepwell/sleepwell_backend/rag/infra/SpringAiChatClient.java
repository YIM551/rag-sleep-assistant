package com.sleepwell.sleepwell_backend.rag.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Primary
@Component
public class SpringAiChatClient implements RagChatClient {

  /** 비스트리밍(동기) 호출용 - OpenAI 지정 */
  private final ChatModel chatModel;

  /** 스트리밍 모델 (OpenAI만 주입, 없으면 동기 호출로 폴백) */
  private final Optional<StreamingChatModel> streamingModel;

  // ✅ 명시 생성자 + @Qualifier로 충돌 제거
  public SpringAiChatClient(
      @Qualifier("openAiChatModel") ChatModel chatModel,
      @Qualifier("openAiStreamingChatModel") ObjectProvider<StreamingChatModel> streamingProvider) {
    this.chatModel = chatModel;
    this.streamingModel = Optional.ofNullable(streamingProvider.getIfAvailable());
  }

  @Override
  public String chat(String system, String user) {
    Prompt prompt = new Prompt(List.of(new SystemMessage(system), new UserMessage(user)));
    ChatResponse res = chatModel.call(prompt);
    try {
      Object out = res.getResult().getOutput();
      return extractAssistantText(out);
    } catch (Exception e) {
      log.warn("chat() returned empty result: errorType={}", e.getClass().getSimpleName());
      return "";
    }
  }

  @Override
  public void chatStream(String system, String user,
      Consumer<String> onDelta,
      Runnable onDone) {
    Prompt prompt = new Prompt(List.of(new SystemMessage(system), new UserMessage(user)));

    // 스트리밍 모델이 있으면 토큰/청크 스트림 사용
    streamingModel.ifPresentOrElse(sm -> {
      final StringBuilder acc = new StringBuilder(); // 누적된 전체 텍스트
      sm.stream(prompt) // Flux<ChatResponse>
          .map(cr -> {
            try {
              Object out = cr.getResult().getOutput();
              return extractAssistantText(out);
            } catch (Exception e) {
              return "";
            }
          })
          .filter(chunk -> chunk != null && !chunk.isEmpty())
          .doOnNext(chunk -> {
            // 누적 텍스트 대비 증분만 계산해서 전달
            String sofar = acc.toString();
            String delta = chunk.startsWith(sofar) ? chunk.substring(sofar.length()) : chunk;
            acc.setLength(0);
            acc.append(chunk);
            if (!delta.isEmpty())
              onDelta.accept(delta);
          })
          .doOnError(e -> log.warn("stream error: errorType={}", e.getClass().getSimpleName()))
          .doFinally(sig -> onDone.run())
          .subscribe();
    }, () -> {
      // 스트리밍 미지원이면 전체를 한 번에
      String full = chat(system, user);
      onDelta.accept(full);
      onDone.run();
    });
  }

  /** AssistantMessage 버전 차이(getContent()/getText()) 흡수 */
  private static String extractAssistantText(Object out) {
    if (out == null)
      return "";
    try {
      var m = out.getClass().getMethod("getContent");
      Object v = m.invoke(out);
      return v == null ? "" : String.valueOf(v);
    } catch (Exception ignore) {
      try {
        var m2 = out.getClass().getMethod("getText");
        Object v2 = m2.invoke(out);
        return v2 == null ? "" : String.valueOf(v2);
      } catch (Exception ignore2) {
        return String.valueOf(out);
      }
    }
  }
}
