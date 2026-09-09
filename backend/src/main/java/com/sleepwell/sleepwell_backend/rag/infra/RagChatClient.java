package com.sleepwell.sleepwell_backend.rag.infra;

import java.util.function.Consumer;

public interface RagChatClient {
    String chat(String system, String user) throws Exception;
    void chatStream(String system, String user, Consumer<String> onDelta, Runnable onDone) throws Exception;
}
