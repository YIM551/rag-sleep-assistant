package com.sleepwell.sleepwell_backend.dto.voice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 텍스트-음성 변환 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextToSpeechRequestDto {

    /**
     * 변환할 텍스트
     */
    @NotBlank(message = "변환할 텍스트는 필수입니다")
    @Size(max = 4096, message = "텍스트는 4096자를 초과할 수 없습니다")
    private String text;

    /**
     * 음성 모델/타입
     * OpenAI: tts-1, tts-1-hd
     * Google: Standard, WaveNet, Neural2
     */
    @Builder.Default
    private String model = "tts-1";

    /**
     * 음성 종류
     * OpenAI: alloy, echo, fable, onyx, nova, shimmer
     * Google: 언어별 다양한 음성 지원
     */
    @Builder.Default
    private String voice = "alloy";

    /**
     * 언어 코드 (ISO 639-1)
     */
    @Builder.Default
    private String language = "ko";

    /**
     * 출력 형식
     * mp3, opus, aac, flac, wav, pcm
     */
    @Builder.Default
    private String format = "mp3";

    /**
     * 음성 속도 (0.25 ~ 4.0)
     */
    @Builder.Default
    private Double speed = 1.0;

    /**
     * 음성 피치 (-20.0 ~ 20.0, Google만 지원)
     */
    private Double pitch;

    /**
     * 볼륨 게인 (-96.0 ~ 16.0 dB, Google만 지원)
     */
    private Double volumeGainDb;

    /**
     * 샘플 레이트 (Hz)
     */
    private Integer sampleRate;

    /**
     * 오디오 품질 (high, standard, low)
     */
    @Builder.Default
    private String quality = "standard";

    /**
     * SSML(Speech Synthesis Markup Language) 사용 여부
     */
    @Builder.Default
    private Boolean enableSsml = false;

    /**
     * 감정 표현 (Google Neural2만 지원)
     */
    private String emotion;

    /**
     * 스트리밍 여부
     */
    @Builder.Default
    private Boolean streaming = false;

    /**
     * 사용자 ID (로깅/통계용)
     */
    private Long userId;

    /**
     * 요청 ID (추적용)
     */
    private String requestId;

    /**
     * 메타데이터
     */
    private String metadata;
}