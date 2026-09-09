package com.sleepwell.sleepwell_backend.controller;

import com.sleepwell.sleepwell_backend.dto.payment.*;
import com.sleepwell.sleepwell_backend.enums.PaymentStatus;
import com.sleepwell.sleepwell_backend.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.security.CustomUserDetails;
import com.sleepwell.sleepwell_backend.exception.BusinessException;

/**
 * 결제 관련 API 컨트롤러
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@Tag(name = "구독 및 결제", description = "결제 관련 API")
@SecurityRequirement(name = "bearer-jwt")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    public PaymentController(@Qualifier("tossPaymentsService") PaymentService paymentService, 
                           UserRepository userRepository) {
        this.paymentService = paymentService;
        this.userRepository = userRepository;
    }

    /**
     * 결제 준비
     */
    @PostMapping("/prepare")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "결제 준비", description = "결제를 위한 준비 단계를 처리합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결제 준비 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "409", description = "중복된 결제 요청")
    })
    public ResponseEntity<PaymentPrepareResponseDto> preparePayment(
            @Valid @RequestBody PaymentPrepareRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        
        log.info("결제 준비 요청 - user: {}, amount: {}", user.getId(), request.getAmount());
        
        // 현재 로그인한 사용자의 ID로 설정
        request.setUserId(user.getId());
        
        PaymentPrepareResponseDto response = paymentService.preparePayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 승인
     */
    @PostMapping("/complete")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "결제 승인", description = "결제 승인을 처리합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결제 승인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "결제 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 처리된 결제")
    })
    public ResponseEntity<PaymentResponseDto> completePayment(
            @Valid @RequestBody PaymentCompleteRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        
        log.info("결제 승인 요청 - user: {}, orderId: {}", user.getId(), request.getOrderId());
        
        PaymentResponseDto response = paymentService.completePayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 취소
     */
    @PostMapping("/cancel")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "결제 취소", description = "완료된 결제를 취소합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결제 취소 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "결제 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "취소할 수 없는 상태")
    })
    public ResponseEntity<PaymentResponseDto> cancelPayment(
            @Valid @RequestBody PaymentCancelRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        
        log.info("결제 취소 요청 - user: {}, orderId: {}", user.getId(), request.getOrderId());
        
        request.setRequesterId(user.getId());
        
        PaymentResponseDto response = paymentService.cancelPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 부분 환불
     */
    @PostMapping("/refund")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "부분 환불", description = "결제 금액의 일부를 환불합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "환불 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (환불 금액 초과 등)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "결제 정보를 찾을 수 없음")
    })
    public ResponseEntity<PaymentRefundResponseDto> refundPayment(
            @Valid @RequestBody PaymentRefundRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        
        log.info("환불 요청 - user: {}, orderId: {}, amount: {}", 
            user.getId(), request.getOrderId(), request.getRefundAmount());
        
        request.setRequesterId(user.getId());
        
        PaymentRefundResponseDto response = paymentService.refundPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 상태 조회
     */
    @GetMapping("/status/{orderId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "결제 상태 조회", description = "특정 결제의 현재 상태를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결제 상태 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "결제 정보를 찾을 수 없음")
    })
    public ResponseEntity<PaymentStatusResponseDto> getPaymentStatus(
            @Parameter(description = "주문번호") @PathVariable String orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        
        log.debug("결제 상태 조회 - user: {}, orderId: {}", user.getId(), orderId);
        
        PaymentStatusResponseDto response = paymentService.getPaymentStatus(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 내역 조회
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "결제 내역 조회", description = "사용자의 결제 내역을 페이징하여 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결제 내역 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<Page<PaymentResponseDto>> getPaymentHistory(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        Long userId = user.getId();
        log.debug("결제 내역 조회 - userId: {}", userId);
        
        Page<PaymentResponseDto> response = paymentService.getPaymentHistory(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 상태의 결제 내역 조회
     */
    @GetMapping("/history/status/{status}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "상태별 결제 내역 조회", description = "특정 상태의 결제 내역을 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태별 결제 내역 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByStatus(
            @Parameter(description = "결제 상태") @PathVariable PaymentStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        Long userId = user.getId();
        log.debug("상태별 결제 내역 조회 - userId: {}, status: {}", userId, status);
        
        List<PaymentResponseDto> response = paymentService.getPaymentsByStatus(userId, status);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 통계 조회
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "결제 통계 조회", description = "기간별 결제 통계를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결제 통계 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (날짜 형식 오류 등)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<PaymentStatisticsDto> getPaymentStatistics(
            @Parameter(description = "시작일시") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "종료일시") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        Long userId = user.getId();
        log.debug("결제 통계 조회 - userId: {}, period: {} ~ {}", userId, startDate, endDate);
        
        // 날짜 범위 검증
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("종료일시가 시작일시보다 이전일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        
        PaymentStatisticsDto response = paymentService.getPaymentStatistics(userId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 검증 (클라이언트 측 검증용)
     */
    @PostMapping("/validate")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "결제 검증", description = "결제 정보의 유효성을 검증합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<Map<String, Boolean>> validatePayment(
            @Valid @RequestBody PaymentValidationRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        User user = userDetails.getUser();
        
        log.debug("결제 검증 요청 - user: {}, orderId: {}", user.getId(), request.getOrderId());
        
        boolean isValid = paymentService.validatePayment(request);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    /**
     * 토스페이먼츠 웹훅 처리
     */
    @PostMapping("/webhook/tosspayments")
    @Operation(summary = "토스페이먼츠 웹훅", description = "토스페이먼츠에서 전송하는 웹훅을 처리합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "웹훅 처리 성공"),
            @ApiResponse(responseCode = "401", description = "서명 누락"),
            @ApiResponse(responseCode = "403", description = "서명 검증 실패")
    })
    public ResponseEntity<Void> processTosspaymentsWebhook(
            @RequestBody PaymentWebhookDto webhook,
            @RequestHeader(value = "Toss-Webhook-Signature", required = false) String signature) {
        
        log.info("토스페이먼츠 웹훅 수신 - eventType: {}", webhook.getEventType());
        
        // 웹훅 서명 검증
        if (signature == null || signature.trim().isEmpty()) {
            log.warn("토스페이먼츠 웹훅 서명이 누락됨");
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        
        // 실제 운영 환경에서는 토스페이먼츠에서 제공하는 서명 검증 로직을 구현해야 함
        // 현재는 기본적인 검증만 수행
        if (!isValidTosspaymentsSignature(webhook, signature)) {
            log.error("토스페이먼츠 웹훅 서명 검증 실패");
            return ResponseEntity.status(403).build(); // Forbidden
        }
        
        paymentService.processWebhook(webhook);
        return ResponseEntity.ok().build();
    }

    /**
     * 아임포트 웹훅 처리
     */
    @PostMapping("/webhook/iamport")
    @Operation(summary = "아임포트 웹훅", description = "아임포트에서 전송하는 웹훅을 처리합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "웹훅 처리 성공"),
            @ApiResponse(responseCode = "401", description = "서명 누락"),
            @ApiResponse(responseCode = "403", description = "서명 검증 실패")
    })
    public ResponseEntity<Void> processIamportWebhook(
            @RequestBody PaymentWebhookDto webhook,
            @RequestHeader(value = "X-IamPort-Signature", required = false) String signature) {
        
        log.info("아임포트 웹훅 수신 - eventType: {}", webhook.getEventType());
        
        // 웹훅 서명 검증
        if (signature == null || signature.trim().isEmpty()) {
            log.warn("아임포트 웹훅 서명이 누락됨");
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        
        // 실제 운영 환경에서는 아임포트에서 제공하는 서명 검증 로직을 구현해야 함
        // 현재는 기본적인 검증만 수행
        if (!isValidIamportSignature(webhook, signature)) {
            log.error("아임포트 웹훅 서명 검증 실패");
            return ResponseEntity.status(403).build(); // Forbidden
        }
        
        paymentService.processWebhook(webhook);
        return ResponseEntity.ok().build();
    }

    /**
     * 관리자용 - 만료된 결제 정리
     */
    @PostMapping("/admin/expire")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "만료된 결제 정리", description = "오래된 대기중인 결제를 만료 처리합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "만료 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 부족")
    })
    public ResponseEntity<Void> expireOldPayments() {
        log.info("만료된 결제 정리 시작");
        
        paymentService.expireOldPayments();
        return ResponseEntity.ok().build();
    }

    /**
     * 토스페이먼츠 웹훅 서명 검증
     * 
     * @param webhook 웹훅 데이터
     * @param signature 웹훅 서명
     * @return 서명이 유효한지 여부
     */
    private boolean isValidTosspaymentsSignature(PaymentWebhookDto webhook, String signature) {
        try {
            // 실제 운영 환경에서는 토스페이먼츠의 공식 서명 검증 로직을 사용해야 함
            // 여기서는 기본적인 검증만 수행
            
            // 1. 서명 형식 확인 (예: 최소 길이 체크)
            if (signature.length() < 10) {
                return false;
            }
            
            // 2. 개발/테스트 환경에서는 임시로 통과
            // 운영 환경에서는 실제 HMAC SHA256 검증을 수행해야 함
            log.debug("토스페이먼츠 웹훅 서명 검증 - 서명 길이: {}", signature.length());
            
            return true; // 임시로 항상 통과
            
        } catch (Exception e) {
            log.error("토스페이먼츠 웹훅 서명 검증 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 아임포트 웹훅 서명 검증
     * 
     * @param webhook 웹훅 데이터
     * @param signature 웹훅 서명
     * @return 서명이 유효한지 여부
     */
    private boolean isValidIamportSignature(PaymentWebhookDto webhook, String signature) {
        try {
            // 실제 운영 환경에서는 아임포트의 공식 서명 검증 로직을 사용해야 함
            // 여기서는 기본적인 검증만 수행
            
            // 1. 서명 형식 확인 (예: 최소 길이 체크)
            if (signature.length() < 10) {
                return false;
            }
            
            // 2. 개발/테스트 환경에서는 임시로 통과
            // 운영 환경에서는 실제 HMAC SHA256 검증을 수행해야 함
            log.debug("아임포트 웹훅 서명 검증 - 서명 길이: {}", signature.length());
            
            return true; // 임시로 항상 통과
            
        } catch (Exception e) {
            log.error("아임포트 웹훅 서명 검증 중 오류 발생", e);
            return false;
        }
    }
}