package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.notification.FcmTokenDto;
import com.sleepwell.sleepwell_backend.dto.notification.PushNotificationRequestDto;
import com.sleepwell.sleepwell_backend.dto.notification.PushNotificationResponseDto;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.security.CustomUserDetails;
import com.sleepwell.sleepwell_backend.service.CustomUserDetailsService;
import com.sleepwell.sleepwell_backend.service.FcmPushService;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * FCM 푸시 알림 관련 API 컨트롤러
 * FCM 토큰 관리, 푸시 알림 발송, 토픽 구독 관리 등의 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
@Validated
@Tag(name = "Firebase Cloud Messaging", description = "FCM 푸시 알림 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class FcmController {

    private final FcmPushService fcmPushService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * 현재 사용자의 FCM 토큰을 업데이트합니다.
     * POST와 PUT 모두 지원합니다.
     */
    @RequestMapping(value = "/token", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "FCM 토큰 업데이트", description = "현재 사용자의 FCM 토큰을 업데이트합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 업데이트 성공",
                    content = @Content(schema = @Schema(implementation = FcmTokenDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 토큰)"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<FcmTokenDto> updateFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "FCM 토큰", required = true)
            @RequestBody @NotBlank(message = "FCM 토큰은 필수입니다") String fcmToken) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        log.info("FCM 토큰 업데이트 요청: userId={}", user.getId());
        
        FcmTokenDto result = fcmPushService.updateUserFcmToken(user.getId(), fcmToken);
        return ResponseEntity.ok(result);
    }

    /**
     * 현재 사용자의 FCM 토큰을 삭제합니다.
     */
    @DeleteMapping("/token")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "FCM 토큰 삭제", description = "현재 사용자의 FCM 토큰을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "토큰 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<Void> deleteFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        log.info("FCM 토큰 삭제 요청: userId={}", user.getId());
        
        fcmPushService.deleteUserFcmToken(user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 특정 토픽을 구독합니다.
     */
    @PostMapping("/topics/{topic}/subscribe")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "토픽 구독", description = "특정 토픽을 구독합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토픽 구독 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<Map<String, Object>> subscribeToTopic(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "구독할 토픽 이름", required = true)
            @PathVariable String topic) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        log.info("토픽 구독 요청: userId={}, topic={}", user.getId(), topic);
        
        fcmPushService.subscribeToTopic(user.getId(), topic);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "토픽 구독이 완료되었습니다.",
                "topic", topic
        ));
    }

    /**
     * 특정 토픽 구독을 해제합니다.
     */
    @PostMapping("/topics/{topic}/unsubscribe")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "토픽 구독 해제", description = "특정 토픽 구독을 해제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토픽 구독 해제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<Map<String, Object>> unsubscribeFromTopic(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "구독 해제할 토픽 이름", required = true)
            @PathVariable String topic) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        log.info("토픽 구독 해제 요청: userId={}, topic={}", user.getId(), topic);
        
        fcmPushService.unsubscribeFromTopic(user.getId(), topic);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "토픽 구독이 해제되었습니다.",
                "topic", topic
        ));
    }

    /**
     * 테스트용 푸시 알림을 발송합니다. (개발 환경에서만 사용)
     */
    @PostMapping("/test")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "테스트 푸시 알림 발송", description = "현재 사용자에게 테스트 푸시 알림을 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "푸시 알림 발송 성공",
                    content = @Content(schema = @Schema(implementation = PushNotificationResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<PushNotificationResponseDto> sendTestNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "테스트 메시지 (선택사항)")
            @RequestParam(required = false, defaultValue = "SleepWell 테스트 알림입니다.") String message) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        log.info("테스트 푸시 알림 발송 요청: userId={}", user.getId());
        
        PushNotificationResponseDto response = fcmPushService.sendToUser(
                user.getId(),
                "테스트 알림",
                message,
                Map.of("type", "test", "timestamp", String.valueOf(System.currentTimeMillis()))
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 관리자용: 특정 사용자에게 푸시 알림을 발송합니다.
     */
    @PostMapping("/admin/send")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "관리자 푸시 알림 발송", description = "관리자가 특정 사용자 또는 그룹에게 푸시 알림을 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "푸시 알림 발송 성공",
                    content = @Content(schema = @Schema(implementation = PushNotificationResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<?> sendAdminNotification(
            @Valid @RequestBody PushNotificationRequestDto request) {
        
        log.info("관리자 푸시 알림 발송 요청: {}", request);
        
        // 다중 사용자 발송인 경우
        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            List<PushNotificationResponseDto> responses = fcmPushService.sendToMultipleUsers(
                    request.getUserIds(),
                    request.getTitle(),
                    request.getBody(),
                    request.getData()
            );
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "totalCount", responses.size(),
                    "successCount", responses.stream().filter(PushNotificationResponseDto::isSuccess).count(),
                    "results", responses
            ));
        }
        
        // 단일 발송
        PushNotificationResponseDto response = fcmPushService.send(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 관리자용: 토픽에 푸시 알림을 발송합니다.
     */
    @PostMapping("/admin/topics/{topic}/send")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "토픽 푸시 알림 발송", description = "관리자가 특정 토픽을 구독하는 모든 사용자에게 푸시 알림을 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "푸시 알림 발송 성공",
                    content = @Content(schema = @Schema(implementation = PushNotificationResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<PushNotificationResponseDto> sendToTopic(
            @Parameter(description = "발송할 토픽 이름", required = true)
            @PathVariable String topic,
            @Valid @RequestBody PushNotificationRequestDto request) {
        
        log.info("토픽 푸시 알림 발송 요청: topic={}, title={}", topic, request.getTitle());
        
        PushNotificationResponseDto response = fcmPushService.sendToTopic(
                topic,
                request.getTitle(),
                request.getBody(),
                request.getData()
        );
        
        return ResponseEntity.ok(response);
    }
}