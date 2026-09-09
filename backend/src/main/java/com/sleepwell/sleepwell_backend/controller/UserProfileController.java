package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.PasswordChangeRequestDto;
import com.sleepwell.sleepwell_backend.dto.UserProfileRequestDto;
import com.sleepwell.sleepwell_backend.dto.UserProfileResponseDto;
import com.sleepwell.sleepwell_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 프로필 관리 컨트롤러
 */
@Tag(name = "사용자 프로필", description = "사용자 프로필 관리 및 설정")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    /**
     * 현재 사용자 프로필 조회
     */
    @Operation(
        summary = "현재 사용자 프로필 조회", 
        description = "JWT 토큰을 통해 인증된 사용자의 프로필 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "프로필 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserProfileResponseDto.class)
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
                        "error": "인증이 필요합니다"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponseDto> getUserProfile(Authentication authentication) {
        String userEmail = getCurrentUserEmail(authentication);
        log.info("사용자 프로필 조회 요청 - 사용자 이메일: {}", userEmail);
        
        UserProfileResponseDto profile = userService.getUserProfileByEmail(userEmail);
        
        log.info("사용자 프로필 조회 성공 - 사용자 이메일: {}", userEmail);
        return ResponseEntity.ok(profile);
    }

    /**
     * 사용자 프로필 업데이트
     */
    @Operation(
        summary = "사용자 프로필 업데이트", 
        description = "현재 사용자의 프로필 정보를 업데이트합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "프로필 업데이트 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserProfileResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 데이터",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "error": "유효하지 않은 데이터입니다"
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
                        "error": "인증이 필요합니다"
                    }
                    """
                )
            )
        )
    })
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponseDto> updateUserProfile(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "프로필 업데이트 정보",
                content = @Content(
                    schema = @Schema(implementation = UserProfileRequestDto.class)
                )
            )
            @RequestBody UserProfileRequestDto request,  // 부분 업데이트이므로 @Valid 제거
            Authentication authentication) {
        String userEmail = getCurrentUserEmail(authentication);
        log.info("사용자 프로필 업데이트 요청 - 사용자 이메일: {}, 새 이메일: {}", userEmail, request.getEmail());
        
        UserProfileResponseDto updatedProfile = userService.updateUserProfileByEmail(userEmail, request);
        
        log.info("사용자 프로필 업데이트 성공 - 사용자 이메일: {}", userEmail);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * 비밀번호 변경
     */
    @Operation(
        summary = "비밀번호 변경", 
        description = "현재 사용자의 비밀번호를 변경합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "비밀번호 변경 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    "비밀번호가 성공적으로 변경되었습니다"
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 데이터",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.sleepwell.sleepwell_backend.exception.ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation Error Example",
                    summary = "유효성 검증 실패 예시",
                    value = """
                    {
                        "timestamp": "2025-07-10T18:00:00.123456",
                        "status": 400,
                        "error": "Validation Failed",
                        "message": "입력 값 유효성 검사에 실패했습니다.",
                        "path": "/api/users/password",
                        "details": {
                            "newPassword": [
                                "비밀번호는 8자 이상, 20자 이하이어야 합니다.",
                                "비밀번호는 대소문자, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다."
                            ],
                            "currentPassword": [
                                "현재 비밀번호는 필수입니다"
                            ],
                            "globalErrors": [
                                "비밀번호와 비밀번호 확인이 일치하지 않습니다"
                            ]
                        }
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
                        "error": "인증이 필요합니다"
                    }
                    """
                )
            )
        )
    })
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "비밀번호 변경 정보",
                content = @Content(
                    schema = @Schema(implementation = PasswordChangeRequestDto.class)
                )
            )
            @Valid @RequestBody PasswordChangeRequestDto request,
            Authentication authentication) {
        String userEmail = getCurrentUserEmail(authentication);
        log.info("비밀번호 변경 요청 - 사용자 이메일: {}", userEmail);

        userService.changePasswordByEmail(userEmail, request.getCurrentPassword(), request.getNewPassword());
        
        log.info("비밀번호 변경 성공 - 사용자 이메일: {}", userEmail);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다");
    }

    /**
     * 계정 비활성화 (소프트 삭제)
     */
    @Operation(
        summary = "계정 비활성화", 
        description = "현재 사용자의 계정을 비활성화합니다 (소프트 삭제)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "계정 비활성화 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    "계정이 성공적으로 비활성화되었습니다"
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
                        "error": "인증이 필요합니다"
                    }
                    """
                )
            )
        )
    })
    @DeleteMapping("/account")
    public ResponseEntity<String> deactivateAccount(Authentication authentication) {
        String userEmail = getCurrentUserEmail(authentication);
        log.warn("계정 비활성화 요청 - 사용자 이메일: {}", userEmail);
        
        userService.deactivateUserByEmail(userEmail);
        
        log.warn("계정 비활성화 완료 - 사용자 이메일: {}", userEmail);
        return ResponseEntity.ok("계정이 성공적으로 비활성화되었습니다");
    }

    /**
     * 인증 객체에서 사용자 이메일 추출
     */
    private String getCurrentUserEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("인증되지 않은 요청");
            throw new IllegalArgumentException("인증이 필요합니다");
        }
        
        Object principal = authentication.getPrincipal();
        String userEmail;
        
        if (principal instanceof UserDetails) {
            userEmail = ((UserDetails) principal).getUsername();
        } else {
            userEmail = authentication.getName();
        }
        
        if (userEmail == null || userEmail.trim().isEmpty()) {
            log.error("유효하지 않은 사용자 이메일: {}", userEmail);
            throw new IllegalArgumentException("유효하지 않은 사용자 이메일입니다");
        }
        
        return userEmail;
    }
} 