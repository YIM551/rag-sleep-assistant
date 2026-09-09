package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메시지 전송 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "메시지 전송 요청")
public class SendMessageRequestDto {

    @NotNull(message = "메시지 타입은 필수입니다")
    @Schema(description = "메시지 타입", example = "USER_TEXT")
    private MessageType messageType;

    @NotBlank(message = "메시지 내용을 입력해주세요")
    @Size(max = 2000, message = "메시지 내용은 2000자를 초과할 수 없습니다")
    @Schema(description = "메시지 내용", example = "구체적으로 어떤 방법이 도움이 될까요?")
    private String content;

    @Schema(description = "음성 파일 경로 (음성 메시지인 경우)", example = "/voice/user_message_123.wav")
    private String voiceFilePath;

    @Schema(description = "음성 메시지 길이 (초)", example = "15")
    private Integer voiceDurationSeconds;

    @Schema(description = "메시지 메타데이터 (JSON 형태)")
    private String metadata;

    @Schema(description = "긴급 메시지 여부", example = "false")
    @Builder.Default
    private Boolean urgent = false;

    @Schema(description = "수면 데이터 포함 일수 (최근 N일)", example = "7")
    @Builder.Default
    private Integer sleepDataDays = 7;
} 