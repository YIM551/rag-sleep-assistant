package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.SimpleMessageResponseDto;
import com.sleepwell.sleepwell_backend.dto.notification.NotificationDto;
import com.sleepwell.sleepwell_backend.dto.notification.CreateNotificationRequestDto;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.security.CustomUserDetails;
import com.sleepwell.sleepwell_backend.enums.NotificationType;
import com.sleepwell.sleepwell_backend.enums.Priority;
import com.sleepwell.sleepwell_backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "알림", description = "알림 관련 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "사용자 알림 목록 조회", description = "현재 로그인한 사용자의 모든 알림을 최신순으로 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<Page<NotificationDto>> getUserNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        return ResponseEntity.ok(notificationService.getUserNotifications(user.getId(), pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 개수 조회", description = "현재 로그인한 사용자의 읽지 않은 알림 개수를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "읽지 않은 알림 개수 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<Map<String, Long>> getUnreadNotificationCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        long count = notificationService.getUnreadNotificationCount(user.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    public ResponseEntity<NotificationDto> markNotificationAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "읽음 처리할 알림의 ID") @PathVariable Long notificationId) {
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        return ResponseEntity.ok(notificationService.markNotificationAsRead(user.getId(), notificationId));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "모든 알림 읽음 처리", description = "로그인한 사용자의 모든 알림을 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "모든 알림 읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<Void> markAllNotificationsAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        notificationService.markAllNotificationsAsRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "알림 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "삭제할 알림의 ID") @PathVariable Long notificationId) {
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        notificationService.deleteNotification(user.getId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/immediate")
    @Operation(summary = "즉시 알림 생성 및 발송", description = "사용자에게 즉시 알림을 생성하고 푸시 알림을 발송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 생성 및 발송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<NotificationDto> createImmediateNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateNotificationRequestDto request) {
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        NotificationDto notification = notificationService.createAndSendNotification(
                user.getId(), request.getType(), request.getTitle(), request.getContent());
        return ResponseEntity.ok(notification);
    }

    @PostMapping("/schedule")
    @Operation(summary = "예약 알림 생성", description = "지정된 시간에 발송될 예약 알림을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "예약 알림 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (과거 시간 등)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<NotificationDto> scheduleNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateNotificationRequestDto request) {
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        NotificationDto notification = notificationService.scheduleNotification(
                user.getId(), request.getType(), request.getTitle(), request.getContent(), 
                request.getScheduledAt(), request.getExpiresAt(), request.getPriority());
        return ResponseEntity.ok(notification);
    }

    @PostMapping("/related")
    @Operation(summary = "관련 데이터 알림 생성", description = "특정 데이터와 연결된 알림을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "관련 데이터 알림 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<NotificationDto> createRelatedNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateNotificationRequestDto request) {
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        NotificationDto notification = notificationService.createRelatedNotification(
                user.getId(), request.getType(), request.getTitle(), request.getContent(), 
                request.getRelatedId(), request.getRelatedType(), request.getPriority());
        return ResponseEntity.ok(notification);
    }
} 