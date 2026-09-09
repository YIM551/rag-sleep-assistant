package com.sleepwell.sleepwell_backend.service.voice.impl;

import com.sleepwell.sleepwell_backend.dto.voice.TextToSpeechRequestDto;
import com.sleepwell.sleepwell_backend.dto.voice.TextToSpeechResponseDto;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.service.voice.TextToSpeechService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI TTS API 기반 텍스트-음성 변환 서비스
 * OpenAI의 고품질 TTS 모델을 사용하여 자연스러운 음성 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAITextToSpeechService implements TextToSpeechService {

    private final RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;

    @Value("${sleepwell.voice.openai.tts.model:tts-1}")
    private String defaultModel;

    @Value("${sleepwell.voice.openai.tts.voice:alloy}")
    private String defaultVoice;

    @Value("${sleepwell.voice.openai.tts.timeout:30000}")
    private int timeoutMs;

    @Value("${sleepwell.voice.openai.tts.max-text-length:4096}")
    private int maxTextLength;

    private static final String TTS_API_URL = "https://api.openai.com/v1/audio/speech";
    
    // OpenAI TTS 지원 음성
    private static final String[] AVAILABLE_VOICES = {
        "alloy", "echo", "fable", "onyx", "nova", "shimmer"
    };
    
    // 지원 언어 (OpenAI TTS는 자동 언어 감지)
    private static final String[] SUPPORTED_LANGUAGES = {
        "ko", "en", "ja", "zh", "de", "es", "fr", "hi", "it", "pt", "ru"
    };
    
    // 지원 형식
    private static final String[] SUPPORTED_FORMATS = {
        "mp3", "opus", "aac", "flac", "wav", "pcm"
    };

    @Override
    public TextToSpeechResponseDto synthesizeSpeech(String text, String voice, String language) {
        TextToSpeechRequestDto request = TextToSpeechRequestDto.builder()
                .text(text)
                .voice(voice != null ? voice : defaultVoice)
                .language(language)
                .model(defaultModel)
                .build();
        
        return synthesizeWithOptions(request);
    }

    @Override
    public TextToSpeechResponseDto synthesizeWithOptions(TextToSpeechRequestDto request) {
        long startTime = System.currentTimeMillis();
        String requestId = request.getRequestId() != null ? request.getRequestId() : UUID.randomUUID().toString();
        
        try {
            // 입력 검증
            validateRequest(request);
            
            // API 요청 준비
            HttpHeaders headers = createHeaders();
            Map<String, Object> requestBody = createRequestBody(request);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            log.info("OpenAI TTS 음성 생성 시작 - 텍스트 길이: {}, 음성: {}, 모델: {}", 
                    request.getText().length(), request.getVoice(), request.getModel());
            
            // API 호출
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    TTS_API_URL, HttpMethod.POST, requestEntity, byte[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return buildSuccessResponse(response.getBody(), request, requestId, startTime);
            } else {
                log.error("OpenAI TTS API 응답 오류 - 상태 코드: {}", response.getStatusCode());
                return buildErrorResponse("OpenAI TTS API 오류: " + response.getStatusCode(), requestId, startTime);
            }
            
        } catch (BusinessException e) {
            // BusinessException은 그대로 전파 (호출자가 처리하도록)
            throw e;
        } catch (Exception e) {
            log.error("OpenAI TTS API 호출 실패: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage(), requestId, startTime);
        }
    }

    @Override
    public String[] getAvailableVoices(String language) {
        // OpenAI TTS는 모든 음성이 다국어 지원
        return AVAILABLE_VOICES.clone();
    }

    @Override
    public String[] getSupportedLanguages() {
        return SUPPORTED_LANGUAGES.clone();
    }

    @Override
    public boolean isAvailable() {
        return openaiApiKey != null && !openaiApiKey.equals("dummy-key-for-testing");
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public boolean isWithinTextLimit(String text) {
        return text != null && text.length() <= maxTextLength;
    }

    /**
     * 요청 유효성 검증
     */
    private void validateRequest(TextToSpeechRequestDto request) {
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "변환할 텍스트가 비어있습니다");
        }
        
        if (!isWithinTextLimit(request.getText())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, 
                    String.format("텍스트 길이가 %d자를 초과합니다", maxTextLength));
        }
        
        // 음성 유효성 검증
        String voice = request.getVoice();
        if (voice != null && !isValidVoice(voice)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, 
                    "지원하지 않는 음성입니다: " + voice);
        }
        
        // 형식 유효성 검증
        String format = request.getFormat();
        if (format != null && !isValidFormat(format)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, 
                    "지원하지 않는 출력 형식입니다: " + format);
        }
        
        // 속도 유효성 검증 (OpenAI TTS는 0.25 ~ 4.0 지원)
        Double speed = request.getSpeed();
        if (speed != null && (speed < 0.25 || speed > 4.0)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, 
                    "음성 속도는 0.25 ~ 4.0 범위여야 합니다");
        }
    }

    /**
     * 음성 유효성 확인
     */
    private boolean isValidVoice(String voice) {
        for (String availableVoice : AVAILABLE_VOICES) {
            if (availableVoice.equalsIgnoreCase(voice)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 형식 유효성 확인
     */
    private boolean isValidFormat(String format) {
        for (String supportedFormat : SUPPORTED_FORMATS) {
            if (supportedFormat.equalsIgnoreCase(format)) {
                return true;
            }
        }
        return false;
    }

    /**
     * HTTP 헤더 생성
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openaiApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * 요청 바디 생성
     */
    private Map<String, Object> createRequestBody(TextToSpeechRequestDto request) {
        Map<String, Object> body = new HashMap<>();
        
        // 필수 필드
        body.put("model", request.getModel() != null ? request.getModel() : defaultModel);
        body.put("input", request.getText());
        body.put("voice", request.getVoice() != null ? request.getVoice() : defaultVoice);
        
        // 선택적 필드
        if (request.getFormat() != null) {
            body.put("response_format", request.getFormat());
        }
        if (request.getSpeed() != null) {
            body.put("speed", request.getSpeed());
        }
        
        return body;
    }

    /**
     * 성공 응답 생성
     */
    private TextToSpeechResponseDto buildSuccessResponse(byte[] audioData, 
                                                       TextToSpeechRequestDto request, 
                                                       String requestId, 
                                                       long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        
        // 오디오 데이터를 Base64로 인코딩
        String base64AudioData = Base64.getEncoder().encodeToString(audioData);
        
        // 오디오 길이 추정 (정확하지 않음, 실제로는 오디오 파일 분석 필요)
        double estimatedDuration = estimateAudioDuration(request.getText(), request.getSpeed());
        
        TextToSpeechResponseDto.TextToSpeechResponseDtoBuilder responseBuilder = 
                TextToSpeechResponseDto.builder()
                .audioData(base64AudioData)
                .audioFormat(request.getFormat() != null ? request.getFormat() : "mp3")
                .audioSizeBytes((long) audioData.length)
                .audioDurationSeconds(estimatedDuration)
                .processingTimeMs(processingTime)
                .model(request.getModel() != null ? request.getModel() : defaultModel)
                .voice(request.getVoice() != null ? request.getVoice() : defaultVoice)
                .language(request.getLanguage())
                .provider(getProviderName())
                .textLength(request.getText().length())
                .timestamp(LocalDateTime.now())
                .requestId(requestId)
                .success(true);

        // 메타데이터 생성
        TextToSpeechResponseDto.AudioMetadata metadata = 
                TextToSpeechResponseDto.AudioMetadata.builder()
                .encoding(request.getFormat() != null ? request.getFormat() : "mp3")
                .speed(request.getSpeed() != null ? request.getSpeed() : 1.0)
                .quality(request.getQuality())
                .build();
        
        responseBuilder.metadata(metadata);

        return responseBuilder.build();
    }

    /**
     * 오디오 길이 추정 (대략적)
     */
    private double estimateAudioDuration(String text, Double speed) {
        if (text == null) return 0.0;
        
        // 평균적으로 한국어는 분당 200자, 영어는 분당 150단어 (약 750자) 기준
        // 실제 속도에 따라 조정
        double baseSpeed = speed != null ? speed : 1.0;
        int charCount = text.length();
        
        // 대략적인 계산 (분당 200자 기준)
        double estimatedMinutes = charCount / (200.0 * baseSpeed);
        return estimatedMinutes * 60.0; // 초 단위로 변환
    }

    /**
     * 에러 응답 생성
     */
    private TextToSpeechResponseDto buildErrorResponse(String errorMessage, String requestId, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        
        return TextToSpeechResponseDto.builder()
                .success(false)
                .errorMessage(errorMessage)
                .provider(getProviderName())
                .processingTimeMs(processingTime)
                .timestamp(LocalDateTime.now())
                .requestId(requestId)
                .build();
    }
}