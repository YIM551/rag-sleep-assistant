package com.sleepwell.sleepwell_backend.service.voice.impl;

import com.sleepwell.sleepwell_backend.dto.voice.SpeechToTextRequestDto;
import com.sleepwell.sleepwell_backend.dto.voice.SpeechToTextResponseDto;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.service.voice.SpeechToTextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * OpenAI Whisper 기반 음성-텍스트 변환 서비스
 * OpenAI API를 사용하여 고품질 음성 인식 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAISpeechToTextService implements SpeechToTextService {

    private final RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;

    @Value("${sleepwell.voice.openai.whisper.model:whisper-1}")
    private String defaultModel;

    @Value("${sleepwell.voice.openai.whisper.timeout:30000}")
    private int timeoutMs;

    @Value("${sleepwell.voice.openai.whisper.max-file-size:25}")
    private int maxFileSizeMB;

    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";
    
    // 지원하는 오디오 형식
    private static final Set<String> SUPPORTED_FORMATS = Set.of(
        "audio/mpeg", "audio/mp3", "audio/wav", "audio/m4a", 
        "audio/ogg", "audio/flac", "audio/webm"
    );

    @Override
    public SpeechToTextResponseDto transcribeAudio(MultipartFile audioFile, String language) {
        SpeechToTextRequestDto request = SpeechToTextRequestDto.builder()
                .audioFile(audioFile)
                .language(language)
                .model(defaultModel)
                .build();
        
        return transcribeWithOptions(request);
    }

    @Override
    public SpeechToTextResponseDto transcribeWithOptions(SpeechToTextRequestDto request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 입력 검증
            validateRequest(request);
            
            // API 요청 준비
            HttpHeaders headers = createHeaders();
            MultiValueMap<String, Object> body = createRequestBody(request);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            log.info("OpenAI Whisper 음성 변환 시작 - 파일: {}, 언어: {}", 
                    request.getAudioFile().getOriginalFilename(), request.getLanguage());
            
            // API 호출
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    WHISPER_API_URL, requestEntity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return buildSuccessResponse(response.getBody(), request, startTime);
            } else {
                log.error("OpenAI API 응답 오류 - 상태 코드: {}", response.getStatusCode());
                return buildErrorResponse("OpenAI API 오류: " + response.getStatusCode(), request.getRequestId(), startTime);
            }
            
        } catch (BusinessException e) {
            // BusinessException은 그대로 전파 (호출자가 처리하도록)
            throw e;
        } catch (IOException e) {
            log.error("오디오 파일 처리 중 오류 발생: {}", e.getMessage(), e);
            return buildErrorResponse("오디오 파일 처리 실패", request.getRequestId(), startTime);
        } catch (Exception e) {
            log.error("OpenAI Whisper API 호출 실패: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage(), request.getRequestId(), startTime);
        }
    }

    @Override
    public boolean isSupportedAudioFormat(String mimeType) {
        return SUPPORTED_FORMATS.contains(mimeType.toLowerCase());
    }

    @Override
    public boolean isAvailable() {
        return openaiApiKey != null && !openaiApiKey.equals("dummy-key-for-testing");
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    /**
     * 요청 유효성 검증
     */
    private void validateRequest(SpeechToTextRequestDto request) throws IOException {
        MultipartFile audioFile = request.getAudioFile();
        
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "음성 파일이 비어있습니다");
        }
        
        // 파일 크기 검증
        long fileSizeMB = audioFile.getSize() / (1024 * 1024);
        if (fileSizeMB > maxFileSizeMB) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, 
                    String.format("파일 크기가 %dMB를 초과합니다", maxFileSizeMB));
        }
        
        // 파일 형식 검증
        String contentType = audioFile.getContentType();
        if (contentType == null || !isSupportedAudioFormat(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, 
                    "지원하지 않는 오디오 형식입니다: " + contentType);
        }
    }

    /**
     * HTTP 헤더 생성
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openaiApiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return headers;
    }

    /**
     * 요청 바디 생성
     */
    private MultiValueMap<String, Object> createRequestBody(SpeechToTextRequestDto request) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // 필수 필드 - ByteArrayResource 사용 (스트림 재사용 문제 해결)
        byte[] audioBytes = request.getAudioFile().getBytes();
        body.add("file", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return request.getAudioFile().getOriginalFilename();
            }
        });
        body.add("model", request.getModel());
        
        // 선택적 필드
        if (request.getLanguage() != null) {
            body.add("language", request.getLanguage());
        }
        if (request.getResponseFormat() != null) {
            body.add("response_format", request.getResponseFormat());
        }
        if (request.getTemperature() != null) {
            body.add("temperature", request.getTemperature().toString());
        }
        if (request.getPrompt() != null && !request.getPrompt().trim().isEmpty()) {
            body.add("prompt", request.getPrompt());
        }
        
        // Whisper 특화 설정
        if (request.getIncludeTimestamps() != null && request.getIncludeTimestamps()) {
            body.add("timestamp_granularities[]", "word");
            body.add("timestamp_granularities[]", "segment");
        }
        
        return body;
    }

    /**
     * 성공 응답 생성
     */
    private SpeechToTextResponseDto buildSuccessResponse(Map<String, Object> apiResponse, 
                                                       SpeechToTextRequestDto request, 
                                                       long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        
        String transcribedText = (String) apiResponse.get("text");
        
        SpeechToTextResponseDto.SpeechToTextResponseDtoBuilder responseBuilder = 
                SpeechToTextResponseDto.builder()
                .text(transcribedText)
                .detectedLanguage(request.getLanguage())
                .processingTimeMs(processingTime)
                .model(request.getModel())
                .provider(getProviderName())
                .timestamp(LocalDateTime.now())
                .requestId(request.getRequestId())
                .success(true);

        // 상세 응답 형식인 경우 추가 정보 추출
        if ("verbose_json".equals(request.getResponseFormat())) {
            extractVerboseResponse(apiResponse, responseBuilder);
        }

        // 메타데이터 생성
        try {
            SpeechToTextResponseDto.TranscriptionMetadata metadata = 
                    SpeechToTextResponseDto.TranscriptionMetadata.builder()
                    .audioFormat(request.getAudioFile().getContentType())
                    .fileSizeBytes(request.getAudioFile().getSize())
                    .wordCount(transcribedText.split("\\s+").length)
                    .profanityFiltered(request.getEnableProfanityFilter())
                    .build();
            
            responseBuilder.metadata(metadata);
        } catch (Exception e) {
            log.warn("메타데이터 생성 중 오류 발생: {}", e.getMessage());
        }

        return responseBuilder.build();
    }

    /**
     * 상세 응답 정보 추출
     */
    @SuppressWarnings("unchecked")
    private void extractVerboseResponse(Map<String, Object> apiResponse, 
                                      SpeechToTextResponseDto.SpeechToTextResponseDtoBuilder responseBuilder) {
        try {
            // 언어 감지 정보
            String language = (String) apiResponse.get("language");
            if (language != null) {
                responseBuilder.detectedLanguage(language);
            }
            
            // 지속 시간 정보
            Number duration = (Number) apiResponse.get("duration");
            if (duration != null) {
                responseBuilder.audioDurationSeconds(duration.doubleValue());
            }
            
            // 세그먼트 정보 추출
            List<Map<String, Object>> segments = (List<Map<String, Object>>) apiResponse.get("segments");
            if (segments != null && !segments.isEmpty()) {
                List<SpeechToTextResponseDto.TranscriptionSegment> segmentList = new ArrayList<>();
                
                for (Map<String, Object> segment : segments) {
                    SpeechToTextResponseDto.TranscriptionSegment transcriptionSegment = 
                            SpeechToTextResponseDto.TranscriptionSegment.builder()
                            .text((String) segment.get("text"))
                            .startTime(((Number) segment.get("start")).doubleValue())
                            .endTime(((Number) segment.get("end")).doubleValue())
                            .build();
                    
                    // 신뢰도 정보가 있는 경우
                    if (segment.containsKey("confidence")) {
                        transcriptionSegment.setConfidence(((Number) segment.get("confidence")).doubleValue());
                    }
                    
                    segmentList.add(transcriptionSegment);
                }
                
                responseBuilder.segments(segmentList);
                
                // 전체 신뢰도 계산 (평균)
                double averageConfidence = segmentList.stream()
                        .filter(s -> s.getConfidence() != null)
                        .mapToDouble(SpeechToTextResponseDto.TranscriptionSegment::getConfidence)
                        .average()
                        .orElse(0.0);
                
                if (averageConfidence > 0) {
                    responseBuilder.confidence(averageConfidence);
                }
            }
            
        } catch (Exception e) {
            log.warn("상세 응답 정보 추출 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 에러 응답 생성
     */
    private SpeechToTextResponseDto buildErrorResponse(String errorMessage, String requestId, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        
        return SpeechToTextResponseDto.builder()
                .success(false)
                .errorMessage(errorMessage)
                .provider(getProviderName())
                .processingTimeMs(processingTime)
                .timestamp(LocalDateTime.now())
                .requestId(requestId)
                .build();
    }
}