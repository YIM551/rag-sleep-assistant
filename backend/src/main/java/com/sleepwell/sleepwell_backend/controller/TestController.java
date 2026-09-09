package com.sleepwell.sleepwell_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JWT 인증 테스트를 위한 컨트롤러
 */
@RestController
@RequestMapping("/api/test")
@Tag(name = "테스트", description = "인증 및 시스템 테스트 API")
public class TestController {

    /**
     * 인증이 필요한 테스트 엔드포인트
     */
    @Operation(
        summary = "JWT 인증 테스트", 
        description = "JWT 토큰을 통한 인증이 정상적으로 동작하는지 테스트합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "인증 테스트 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "message": "인증 성공!",
                        "user": "test@sleepwell.com",
                        "authorities": ["ROLE_USER"],
                        "authenticated": true
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "message": "인증 실패!",
                        "authenticated": false
                    }
                    """
                )
            )
        )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> testAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of(
                "message", "인증 성공!",
                "user", authentication.getName(),
                "authorities", authentication.getAuthorities(),
                "authenticated", true
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "message", "인증 실패!",
                "authenticated", false
            ));
        }
    }

    /**
     * 인증이 필요없는 공개 테스트 엔드포인트
     */
    @Operation(
        summary = "공개 API 테스트", 
        description = "인증이 필요없는 공개 API 엔드포인트가 정상적으로 동작하는지 테스트합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "공개 API 테스트 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "message": "공개 API 접근 성공!",
                        "status": "ok"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> testPublic() {
        return ResponseEntity.ok(Map.of(
            "message", "공개 API 접근 성공!",
            "status", "ok"
        ));
    }

    /**
     * 서버 상태 확인 엔드포인트
     */
    @Operation(
        summary = "서버 상태 확인", 
        description = "서버가 정상적으로 동작하는지 확인합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "서버 정상",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "status": "UP",
                        "message": "서버가 정상적으로 작동 중입니다."
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "message", "서버가 정상적으로 작동 중입니다."
        ));
    }

    /**
     * API 버전 확인 엔드포인트
     */
    @Operation(
        summary = "API 버전 확인", 
        description = "현재 API 버전 정보를 확인합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "버전 정보 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "version": "1.0.0",
                        "buildTime": "2025-07-30T00:00:00",
                        "description": "SleepWell Backend API"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> version() {
        return ResponseEntity.ok(Map.of(
            "version", "1.0.0",
            "buildTime", "2025-07-30T00:00:00",
            "description", "SleepWell Backend API"
        ));
    }
} 