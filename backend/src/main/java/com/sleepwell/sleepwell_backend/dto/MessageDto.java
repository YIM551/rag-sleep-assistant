package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.MessageStatus;
import com.sleepwell.sleepwell_backend.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대화 메시지 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대화 메시지")
public class MessageDto {

    @Schema(description = "메시지 ID", example = "1")
    private Long id;

    @Schema(description = "메시지 타입", example = "USER_TEXT")
    private MessageType messageType;

    @Schema(description = "메시지 내용")
    private String content;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    @Schema(description = "메시지 상태")
    private MessageStatus status;
}
