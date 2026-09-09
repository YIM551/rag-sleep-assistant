package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.enums.RagRouteType;
import com.sleepwell.sleepwell_backend.repository.ConversationMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 세션 메시지 카운트를 기반으로 turnIndex를 계산하고 라우팅을 결정합니다.
 * turnIndex = 0(첫 사용자 메시지)은 무조건 RAG 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagTurnRoutingService {

    private final ConversationMessageRepository conversationMessageRepository;
    private final RagRoutingDecider ragRoutingDecider;

    public RagRouteType resolveRouteType(Long sessionId, String message) {
        int messageCount = conversationMessageRepository.countByConsultationSessionId(sessionId);
        int turnIndex = Math.max(0, messageCount / 2);

        if (turnIndex == 0) {
            log.info("[라우팅] turnIndex=0 → RAG 강제 적용 (sessionId={})", sessionId);
            return RagRouteType.RAG;
        }

        RagRouteType decided = ragRoutingDecider.decide(message);
        log.info("[라우팅] turnIndex={}, routeType={}, sessionId={}", turnIndex, decided, sessionId);
        return decided;
    }
}
