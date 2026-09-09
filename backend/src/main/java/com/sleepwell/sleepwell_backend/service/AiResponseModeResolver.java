package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.entity.ConversationMessage;
import com.sleepwell.sleepwell_backend.enums.AiResponseMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class AiResponseModeResolver {

    private static final String[] PINGPONG_KEYWORDS = {
            "한 문장", "짧게", "간단히", "질문하면서", "천천히", "핑퐁", "대화형", "한줄"
    };

    private static final String[] FULL_KEYWORDS = {
            "자세히", "상세히", "전체", "플랜", "정리", "구체적으로", "길게", "완전하게"
    };

    public AiResponseMode resolve(AiResponseMode requestedMode, String userMessage, List<ConversationMessage> recentMessages) {
        if (requestedMode != null && requestedMode != AiResponseMode.AUTO) {
            log.debug("[응답모드] 요청 모드 우선 적용: {}", requestedMode);
            return requestedMode;
        }

        String normalized = userMessage == null ? "" : userMessage.toLowerCase(Locale.KOREAN);

        if (containsKeyword(normalized, PINGPONG_KEYWORDS)) {
            return AiResponseMode.PINGPONG;
        }

        if (containsKeyword(normalized, FULL_KEYWORDS)) {
            return AiResponseMode.FULL;
        }

        if (recentMessages != null && !recentMessages.isEmpty()) {
            ConversationMessage lastMessage = recentMessages.get(recentMessages.size() - 1);
            String content = lastMessage.getContent();
            if (content != null && content.length() <= 250 && content.contains("?")) {
                return AiResponseMode.PINGPONG;
            }
        }

        return AiResponseMode.FULL;
    }

    private boolean containsKeyword(String message, String[] keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword.toLowerCase(Locale.KOREAN))) {
                return true;
            }
        }
        return false;
    }
}
