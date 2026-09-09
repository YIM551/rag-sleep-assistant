package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.voice.SpeechToTextRequestDto;
import com.sleepwell.sleepwell_backend.dto.voice.SpeechToTextResponseDto;
import com.sleepwell.sleepwell_backend.dto.voice.TextToSpeechRequestDto;
import com.sleepwell.sleepwell_backend.dto.voice.TextToSpeechResponseDto;
import com.sleepwell.sleepwell_backend.service.voice.SpeechToTextService;
import com.sleepwell.sleepwell_backend.service.voice.TextToSpeechService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * 음성 처리 컨트롤러
 * STT(Speech-to-Text) 및 TTS(Text-to-Speech) 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@Tag(name = "음성 처리", description = "음성 처리 API (STT/TTS)")
public class VoiceProcessingController {

    @Qualifier("openAISpeechToTextService")
    private final SpeechToTextService speechToTextService;

    @Qualifier("openAITextToSpeechService")
    private final TextToSpeechService textToSpeechService;

    @Operation(
        summary = "음성을 텍스트로 변환 (STT)",
        description = "OpenAI Whisper API를 사용하여 음성 파일을 텍스트로 변환합니다. " +
                     "MP3, WAV, M4A, OGG 등 다양한 형식을 지원합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "음성 변환 성공",
                content = @Content(schema = @Schema(implementation = SpeechToTextResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (파일 형식 오류, 크기 초과 등)"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "413", description = "파일 크기 초과"),
        @ApiResponse(responseCode = "502", description = "OpenAI API 오류"),
        @ApiResponse(responseCode = "503", description = "AI 서비스 일시적 사용 불가")
    })
    @PostMapping(value = "/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SpeechToTextResponseDto> speechToText(
            @Parameter(description = "음성 파일 (MP3, WAV, M4A, OGG 등)", required = true)
            @RequestPart("audioFile") MultipartFile audioFile,
            
            @Parameter(description = "언어 코드 (ko, en, ja 등)")
            @RequestParam(value = "language", defaultValue = "ko") String language,
            
            @Parameter(description = "변환 모델")
            @RequestParam(value = "model", defaultValue = "whisper-1") String model,
            
            @Parameter(description = "응답 형식 (json, text, verbose_json)")
            @RequestParam(value = "responseFormat", defaultValue = "verbose_json") String responseFormat,
            
            @Parameter(description = "타임스탬프 포함 여부")
            @RequestParam(value = "includeTimestamps", defaultValue = "true") Boolean includeTimestamps,
            
            @Parameter(description = "온도 설정 (0.0 ~ 1.0)")
            @RequestParam(value = "temperature", defaultValue = "0.0") Double temperature,
            
            @Parameter(description = "사용자 ID")
            @RequestParam(value = "userId", required = false) Long userId) {

        String requestId = UUID.randomUUID().toString();
        
        log.info("음성-텍스트 변환 요청 - 파일: {}, 언어: {}, 사용자: {}, 요청ID: {}", 
                audioFile.getOriginalFilename(), language, userId, requestId);

        SpeechToTextRequestDto request = SpeechToTextRequestDto.builder()
                .audioFile(audioFile)
                .language(language)
                .model(model)
                .responseFormat(responseFormat)
                .includeTimestamps(includeTimestamps)
                .temperature(temperature)
                .userId(userId)
                .requestId(requestId)
                .build();

        SpeechToTextResponseDto response = speechToTextService.transcribeWithOptions(request);
        
        log.info("음성-텍스트 변환 완료 - 요청ID: {}, 성공: {}, 처리시간: {}ms", 
                requestId, response.getSuccess(), response.getProcessingTimeMs());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "간단한 음성-텍스트 변환",
        description = "기본 설정으로 음성 파일을 텍스트로 변환합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "음성 인식 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (지원되지 않는 형식 등)"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "502", description = "API 오류"),
        @ApiResponse(responseCode = "503", description = "AI 서비스 일시적 사용 불가")
    })
    @PostMapping(value = "/stt/simple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SpeechToTextResponseDto> simpleSpeechToText(
            @Parameter(description = "음성 파일", required = true)
            @RequestPart("audioFile") MultipartFile audioFile,
            
            @Parameter(description = "언어 코드")
            @RequestParam(value = "language", defaultValue = "ko") String language) {

        log.info("간단 음성-텍스트 변환 요청 - 파일: {}, 언어: {}", 
                audioFile.getOriginalFilename(), language);

        SpeechToTextResponseDto response = speechToTextService.transcribeAudio(audioFile, language);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "텍스트를 음성으로 변환 (TTS)",
        description = "OpenAI TTS API를 사용하여 텍스트를 자연스러운 음성으로 변환합니다. " +
                     "다양한 음성 타입과 언어를 지원합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "음성 생성 성공",
                content = @Content(schema = @Schema(implementation = TextToSpeechResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (텍스트 길이 초과, 잘못된 음성 타입 등)"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "502", description = "OpenAI API 오류"),
        @ApiResponse(responseCode = "503", description = "AI 서비스 일시적 사용 불가")
    })
    @PostMapping("/tts")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TextToSpeechResponseDto> textToSpeech(
            @Parameter(description = "TTS 변환 요청 정보", required = true)
            @Valid @RequestBody TextToSpeechRequestDto request) {

        String requestId = request.getRequestId() != null ? request.getRequestId() : UUID.randomUUID().toString();
        request.setRequestId(requestId);
        
        log.info("텍스트-음성 변환 요청 - 텍스트 길이: {}, 음성: {}, 사용자: {}, 요청ID: {}", 
                request.getText().length(), request.getVoice(), request.getUserId(), requestId);

        TextToSpeechResponseDto response = textToSpeechService.synthesizeWithOptions(request);
        
        log.info("텍스트-음성 변환 완료 - 요청ID: {}, 성공: {}, 처리시간: {}ms", 
                requestId, response.getSuccess(), response.getProcessingTimeMs());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "간단한 텍스트-음성 변환",
        description = "기본 설정으로 텍스트를 음성으로 변환합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "음성 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (텍스트 길이 초과 등)"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "502", description = "API 오류"),
        @ApiResponse(responseCode = "503", description = "AI 서비스 일시적 사용 불가")
    })
    @PostMapping("/tts/simple")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TextToSpeechResponseDto> simpleTextToSpeech(
            @Parameter(description = "변환할 텍스트", required = true)
            @RequestParam("text") String text,
            
            @Parameter(description = "음성 타입 (alloy, echo, fable, onyx, nova, shimmer)")
            @RequestParam(value = "voice", defaultValue = "alloy") String voice,
            
            @Parameter(description = "언어 코드")
            @RequestParam(value = "language", defaultValue = "ko") String language) {

        log.info("간단 텍스트-음성 변환 요청 - 텍스트 길이: {}, 음성: {}", text.length(), voice);

        TextToSpeechResponseDto response = textToSpeechService.synthesizeSpeech(text, voice, language);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "사용 가능한 음성 목록 조회",
        description = "TTS에서 사용 가능한 음성 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "음성 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/tts/voices")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getAvailableVoices(
            @Parameter(description = "언어 코드 (선택사항)")
            @RequestParam(value = "language", required = false) String language) {

        String[] voices = textToSpeechService.getAvailableVoices(language);
        String[] supportedLanguages = textToSpeechService.getSupportedLanguages();
        
        Map<String, Object> response = Map.of(
            "voices", voices,
            "supportedLanguages", supportedLanguages,
            "provider", textToSpeechService.getProviderName()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "음성 서비스 상태 확인",
        description = "STT 및 TTS 서비스의 사용 가능 상태를 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상태 조회 성공")
    })
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getVoiceServiceStatus() {
        
        Map<String, Object> status = Map.of(
            "stt", Map.of(
                "provider", speechToTextService.getProviderName(),
                "available", speechToTextService.isAvailable()
            ),
            "tts", Map.of(
                "provider", textToSpeechService.getProviderName(),
                "available", textToSpeechService.isAvailable()
            ),
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(status);
    }

    @Operation(
        summary = "음성 형식 지원 여부 확인",
        description = "특정 오디오 MIME 타입의 지원 여부를 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "형식 지원 여부 확인 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/stt/format-support")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> checkFormatSupport(
            @Parameter(description = "MIME 타입 (예: audio/mpeg, audio/wav)")
            @RequestParam("mimeType") String mimeType) {

        boolean supported = speechToTextService.isSupportedAudioFormat(mimeType);
        
        Map<String, Object> response = Map.of(
            "mimeType", mimeType,
            "supported", supported,
            "provider", speechToTextService.getProviderName()
        );

        return ResponseEntity.ok(response);
    }
}