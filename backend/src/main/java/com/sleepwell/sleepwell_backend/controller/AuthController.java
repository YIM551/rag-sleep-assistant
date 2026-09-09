package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.auth.AuthResponseDto;
import com.sleepwell.sleepwell_backend.dto.auth.LoginRequestDto;
import com.sleepwell.sleepwell_backend.dto.auth.RegisterRequestDto;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.UserRole;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.util.JwtTokenProvider;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Optional;

/**
 * 인증 관련 REST API 컨트롤러
 * 
 * JWT 기반 인증 시스템을 통한 사용자 인증 및 권한 관리를 담당합니다.
 * Spring Security와 연동하여 안전한 인증 플로우를 제공합니다.
 * 
 * 주요 기능:
 * - 사용자 로그인 및 JWT 토큰 발급
 * - 신규 사용자 회원가입
 * - JWT 토큰 유효성 검증
 * - 현재 로그인 사용자 정보 조회
 * 
 * 보안 고려사항:
 * - BCrypt를 통한 비밀번호 암호화
 * - JWT 토큰 기반 무상태 인증
 * - 로그인 실패 시 보안 로그 기록
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see JwtTokenProvider
 */
@Tag(name = "인증", description = "사용자 로그인, 회원가입, 토큰 검증 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 로그인
     */
    @Operation(
        summary = "사용자 로그인", 
        description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "로그인 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponseDto.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "token": "eyJhbGciOiJIUzUxMiJ9...",
                        "email": "test@sleepwell.com",
                        "name": "테스트 사용자",
                        "role": "USER"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "인증 실패 (잘못된 이메일 또는 비밀번호)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.sleepwell.sleepwell_backend.exception.ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "로그인 실패 (유효성 검사 실패)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.sleepwell.sleepwell_backend.exception.ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation Error Example",
                    summary = "유효성 검증 실패 예시",
                    value = """
                    {
                        "timestamp": "2025-07-11T10:00:00.123456",
                        "status": 400,
                        "error": "Validation Failed",
                        "message": "입력 값 유효성 검사에 실패했습니다.",
                        "path": "/api/auth/login",
                        "details": {
                            "email": [
                                "유효한 이메일 형식이 아닙니다"
                            ],
                            "password": [
                                "비밀번호는 필수입니다"
                            ]
                        }
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "로그인 정보",
            content = @Content(
                schema = @Schema(implementation = LoginRequestDto.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "email": "test@sleepwell.com",
                        "password": "test1234"
                    }
                    """
                )
            )
        )
        @Valid @RequestBody LoginRequestDto request) {
            
        log.debug("로그인 시도: {}", request.getEmail());

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new BadCredentialsException("사용자를 찾을 수 없습니다."));

        String token = jwtTokenProvider.createToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        user.updateLastLoginAt();
        userRepository.save(user);

        log.info("로그인 성공: {}", request.getEmail());

        AuthResponseDto response = new AuthResponseDto(
            token,
            user.getEmail(),
            user.getName(),
            user.getRole().name()
        );
        response.setRefreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원가입
     */
    @Operation(
        summary = "사용자 회원가입", 
        description = "이메일, 비밀번호, 이름으로 새로운 계정을 생성합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "회원가입 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "회원가입 실패 (이미 존재하는 이메일 또는 유효성 검사 실패)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.sleepwell.sleepwell_backend.exception.ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation Error Example",
                    summary = "유효성 검증 실패 예시",
                    value = """
                    {
                        "timestamp": "2025-07-11T10:05:00.123456",
                        "status": 400,
                        "error": "Validation Failed",
                        "message": "입력 값 유효성 검사에 실패했습니다.",
                        "path": "/api/auth/register",
                        "details": {
                            "password": [
                                "비밀번호는 8자 이상 20자 이하여야 합니다",
                                "비밀번호는 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다"
                            ],
                            "name": [
                                "이름은 필수입니다"
                            ],
                            "email": [
                                "유효한 이메일 형식이 아닙니다"
                            ]
                        }
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "회원가입 정보",
            content = @Content(
                schema = @Schema(implementation = RegisterRequestDto.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "email": "newuser@sleepwell.com",
                        "password": "NewPass123!",
                        "name": "새 사용자"
                    }
                    """
                )
            )
        )
        @Valid @RequestBody RegisterRequestDto request) {
            
        log.debug("회원가입 시도: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("이미 사용 중인 이메일입니다", HttpStatus.CONFLICT, "EMAIL_DUPLICATE");
        }

        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .role(UserRole.USER)
            .isActive(true)
            .marketingConsent(false)
            .build();

        User savedUser = userRepository.save(user);

        String token = jwtTokenProvider.createToken(savedUser.getEmail(), savedUser.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(savedUser.getEmail());

        log.info("회원가입 성공: {}", request.getEmail());

        AuthResponseDto response = new AuthResponseDto(
            token,
            savedUser.getEmail(),
            savedUser.getName(),
            savedUser.getRole().name()
        );
        response.setRefreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * JWT 토큰 검증
     */
    @Operation(
        summary = "JWT 토큰 검증", 
        description = "JWT 토큰의 유효성을 검증합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "토큰 검증 결과"
        )
    })
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(
        @Parameter(description = "JWT 토큰 (Bearer 형식)", required = true)
        @RequestHeader("Authorization") String bearerToken) {

        String token = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken;
        boolean isValid = jwtTokenProvider.validateToken(token);

        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("valid", false, "message", "Invalid or expired token"));
        }

        return ResponseEntity.ok(Map.of("valid", true, "message", "Token is valid"));
    }

    /**
     * 현재 사용자 정보 조회
     */
    @Operation(
        summary = "현재 사용자 정보 조회", 
        description = "JWT 토큰을 통해 현재 로그인한 사용자의 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "사용자 정보 조회 성공"
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "인증 실패"
        )
    })
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal().equals("anonymousUser")) {
            throw new BadCredentialsException("인증 정보가 없습니다.");
        }
        
        // Principal이 CustomUserDetails인지 확인
        Object principal = authentication.getPrincipal();
        User user = null;
        
        if (principal instanceof CustomUserDetails) {
            user = ((CustomUserDetails) principal).getUser();
        } else if (principal instanceof String) {
            // 이메일로 사용자 조회
            String email = (String) principal;
            user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("사용자를 찾을 수 없습니다."));
        } else {
            throw new BadCredentialsException("알 수 없는 인증 타입입니다.");
        }
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("name", user.getName());
        userInfo.put("role", user.getRole().name());
        userInfo.put("isActive", user.getIsActive());
        userInfo.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);

        return ResponseEntity.ok(userInfo);
    }
    
    /**
     * JWT 토큰 갱신
     */
    @Operation(
        summary = "JWT 토큰 갱신",
        description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "토큰 갱신 성공"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "유효하지 않은 Refresh Token"
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestBody Map<String, String> request) {
        
        String refreshToken = request.get("refreshToken");
        
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("유효하지 않은 Refresh Token입니다.");
        }
        
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("사용자를 찾을 수 없습니다."));
        
        String newAccessToken = jwtTokenProvider.createToken(user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
        
        log.info("토큰 갱신 성공: {}", email);
        
        Map<String, String> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        response.put("refreshToken", newRefreshToken);

        return ResponseEntity.ok(response);
    }

    /**
     * OAuth2 로그인 리다이렉트 콜백 엔드포인트
     *
     * 소셜 로그인(Google, Kakao, Naver) 성공 후 JWT 토큰을 전달받는 엔드포인트입니다.
     * OAuth2AuthenticationSuccessHandler에서 이 URL로 리다이렉트하며,
     * Flutter WebView에서 이 URL을 감지하여 토큰을 추출합니다.
     *
     * 기존 로그인 API와 동일한 AuthResponseDto 형식으로 응답하여
     * Flutter 앱에서 일관된 방식으로 토큰을 처리할 수 있습니다.
     *
     * 사용 플로우:
     * 1. 사용자가 /oauth2/authorization/{provider} 접속 (Google/Kakao/Naver)
     * 2. OAuth2 제공자의 로그인 페이지로 리다이렉트
     * 3. 사용자가 로그인 완료
     * 4. 백엔드에서 JWT 토큰 생성
     * 5. 이 엔드포인트로 리다이렉트하여 토큰 전달
     * 6. Flutter WebView가 이 URL을 감지하여 JSON 응답 파싱
     *
     * @param token JWT 액세스 토큰 (로그인 성공 시)
     * @param error 에러 코드 (로그인 실패 시)
     * @return AuthResponseDto 또는 에러 정보
     */
    @GetMapping("/oauth2/redirect")
    @Operation(
        summary = "OAuth2 리다이렉트 콜백 (JSON 방식)",
        description = """
            소셜 로그인 성공 후 JWT 토큰을 받는 엔드포인트입니다.

            ## 📱 Flutter WebView 사용법

            ### 1. OAuth 로그인 시작
            ```dart
            WebViewController()
              ..loadRequest(Uri.parse(
                'https://api.restdawn.com/oauth2/authorization/google'
              ));
            ```

            ### 2. 리다이렉트 URL 감지
            ```dart
            onPageFinished: (String url) async {
              if (url.contains('/oauth2/redirect')) {
                // JSON 응답 읽기
                final json = await webView.runJavaScriptReturningResult(
                  'document.body.innerText'
                );

                // 파싱 (기존 login API와 동일한 형식)
                final data = jsonDecode(json);
                final token = data['token'];
                final email = data['email'];
                final name = data['name'];
                final role = data['role'];

                // 토큰 저장 및 메인 화면으로
                await saveToken(token);
                Navigator.pushReplacementNamed(context, '/home');
              }
            }
            ```

            ## 🔄 지원하는 OAuth 제공자
            - **Google**: `/oauth2/authorization/google`
            - **Kakao**: `/oauth2/authorization/kakao`
            - **Naver**: `/oauth2/authorization/naver`

            ## ⚡ Deep Link 방식 사용하기
            더 자연스러운 UX를 원한다면 Deep Link 방식을 사용하세요:
            ```
            /oauth2/authorization/google?mode=deeplink
            ```
            → `sleepwell://oauth?token=xxx` 로 앱에 직접 전달

            상세 가이드: [FLUTTER_OAUTH_GUIDE.md](../FLUTTER_OAUTH_GUIDE.md)
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "OAuth2 로그인 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponseDto.class),
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = """
                    {
                      "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGdtYWlsLmNvbSIsInJvbGUiOiJVU0VSIiwidHlwZSI6ImFjY2VzcyIsImlhdCI6MTYzMDAwMDAwMCwiZXhwIjoxNjMwMDg2NDAwfQ...",
                      "email": "portfolio@example.com",
                      "name": "홍길동",
                      "role": "USER",
                      "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGdtYWlsLmNvbSIsInR5cGUiOiJyZWZyZXNoIiwiaWF0IjoxNjMwMDAwMDAwLCJleHAiOjE2MzA2MDQ4MDB9..."
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "OAuth2 로그인 실패 또는 토큰 검증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "에러 발생",
                        value = "{\"error\": \"authentication_failed\"}"
                    ),
                    @ExampleObject(
                        name = "토큰 없음",
                        value = "{\"error\": \"no_token_provided\"}"
                    ),
                    @ExampleObject(
                        name = "유효하지 않은 토큰",
                        value = "{\"error\": \"invalid_token\"}"
                    )
                }
            )
        )
    })
    public ResponseEntity<?> oauth2Redirect(
            @Parameter(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzUxMiJ9...")
            @RequestParam(required = false) String token,

            @Parameter(description = "에러 코드", example = "authentication_failed")
            @RequestParam(required = false) String error) {

        // 에러가 있는 경우
        if (error != null && !error.isEmpty()) {
            log.warn("OAuth2 리다이렉트 - 에러 발생: {}", error);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", error);
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // 토큰이 없는 경우
        if (token == null || token.isEmpty()) {
            log.warn("OAuth2 리다이렉트 - 토큰 없음");
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "no_token_provided");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // 토큰에서 사용자 정보 추출
        try {
            String email = jwtTokenProvider.getEmailFromToken(token);
            Optional<User> userOptional = userRepository.findByEmail(email);

            if (userOptional.isEmpty()) {
                log.warn("OAuth2 리다이렉트 - 사용자를 찾을 수 없음: {}", email);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "user_not_found");
                errorResponse.put("message", "사용자 정보를 찾을 수 없습니다. 다시 회원가입이 필요할 수 있습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            User user = userOptional.get();

            // Refresh Token 생성
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

            // 마지막 로그인 시간 업데이트
            user.updateLastLoginAt();
            userRepository.save(user);

            log.info("OAuth2 리다이렉트 - 로그인 성공: {}", email);

            // 기존 로그인과 동일한 응답 형식
            AuthResponseDto response = new AuthResponseDto(
                token,
                user.getEmail(),
                user.getName(),
                user.getRole().name()
            );
            response.setRefreshToken(refreshToken);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("OAuth2 리다이렉트 - 토큰 검증 실패: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "invalid_token");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 