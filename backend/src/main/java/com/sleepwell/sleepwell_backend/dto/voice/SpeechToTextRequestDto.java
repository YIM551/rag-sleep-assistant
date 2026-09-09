package com.sleepwell.sleepwell_backend.dto.voice;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * 음성-텍스트 변환 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechToTextRequestDto {

    /**
     * 음성 파일
     */
    @NotNull(message = "음성 파일은 필수입니다")
    private MultipartFile audioFile;

    /**
     * 언어 코드 (ISO 639-1)
     * 예: ko (한국어), en (영어), ja (일본어)
     */
    @Builder.Default
    private String language = "ko";

    /**
     * 음성 모델 (OpenAI의 경우: whisper-1)
     */
    @Builder.Default
    private String model = "whisper-1";

    /**
     * 변환 형식 (json, text, verbose_json 등)
     */
    @Builder.Default
    private String responseFormat = "json";

    /**
     * 온도 설정 (0.0 ~ 1.0, 높을수록 더 창의적)
     */
    @Builder.Default
    private Double temperature = 0.0;

    /**
     * 프롬프트 (컨텍스트 제공)
     */
    private String prompt;

    /**
     * 타임스탬프 포함 여부
     */
    @Builder.Default
    private Boolean includeTimestamps = false;

    /**
     * 신뢰도 점수 포함 여부
     */
    @Builder.Default
    private Boolean includeConfidence = false;

    /**
     * 화자 구분 여부 (다중 화자 감지)
     */
    @Builder.Default
    private Boolean speakerDiarization = false;

    /**
     * 필터링 옵션 (욕설, 민감한 내용 등)
     */
    @Builder.Default
    private Boolean enableProfanityFilter = true;

    /**
     * 사용자 ID (로깅/통계용)
     */
    private Long userId;

    /**
     * 요청 ID (추적용)
     */
    private String requestId;
}