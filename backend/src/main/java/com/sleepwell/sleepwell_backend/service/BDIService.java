package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.BDIResponseDto;
import com.sleepwell.sleepwell_backend.dto.BDIResponseRequestDto;
import com.sleepwell.sleepwell_backend.entity.BDIResponse;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.repository.BDIResponseRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * BDI-II 설문 서비스
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class BDIService {

    private final BDIResponseRepository bdiResponseRepository;
    private final UserRepository userRepository;

    /**
     * BDI-II 설문 응답 제출
     */
    @Transactional
    public BDIResponseDto submitResponse(Long userId, BDIResponseRequestDto requestDto) {
        log.info("BDI-II 설문 응답 제출 시작: 사용자 ID {}", userId);

        // DTO 유효성 검증
        requestDto.validateDataIntegrity();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 총점 계산
        int totalScore = requestDto.getQ1() + requestDto.getQ2() + requestDto.getQ3() + requestDto.getQ4() +
                        requestDto.getQ5() + requestDto.getQ6() + requestDto.getQ7() + requestDto.getQ8() +
                        requestDto.getQ9() + requestDto.getQ10() + requestDto.getQ11() + requestDto.getQ12() +
                        requestDto.getQ13() + requestDto.getQ14() + requestDto.getQ15() + requestDto.getQ16() +
                        requestDto.getQ17() + requestDto.getQ18() + requestDto.getQ19() + requestDto.getQ20() +
                        requestDto.getQ21();

        // 자살 위험도 계산 (q9 >= 2)
        boolean suicideRisk = requestDto.getQ9() >= 2;

        // 해석 생성
        String interpretation = generateInterpretation(totalScore);

        // Entity 생성 및 저장
        BDIResponse response = BDIResponse.builder()
                .user(user)
                .q1(requestDto.getQ1())
                .q2(requestDto.getQ2())
                .q3(requestDto.getQ3())
                .q4(requestDto.getQ4())
                .q5(requestDto.getQ5())
                .q6(requestDto.getQ6())
                .q7(requestDto.getQ7())
                .q8(requestDto.getQ8())
                .q9(requestDto.getQ9())
                .q10(requestDto.getQ10())
                .q11(requestDto.getQ11())
                .q12(requestDto.getQ12())
                .q13(requestDto.getQ13())
                .q14(requestDto.getQ14())
                .q15(requestDto.getQ15())
                .q16(requestDto.getQ16())
                .q17(requestDto.getQ17())
                .q18(requestDto.getQ18())
                .q19(requestDto.getQ19())
                .q20(requestDto.getQ20())
                .q21(requestDto.getQ21())
                .totalScore(totalScore)
                .interpretation(interpretation)
                .suicideRisk(suicideRisk)
                .build();

        response.validateResponses();
        BDIResponse savedResponse = bdiResponseRepository.save(response);

        // 자살 위험도 경고 로깅
        if (suicideRisk) {
            log.warn("⚠️ [CRITICAL] 자살 위험 감지 - 사용자 ID: {}, 응답 ID: {}, q9 점수: {}, 총점: {}",
                    userId, savedResponse.getId(), requestDto.getQ9(), totalScore);
            // TODO: 관리자 알림 전송 (향후 구현)
        }

        log.info("BDI-II 설문 응답 저장 완료: 응답 ID {}, 사용자 ID {}, 총점 {}, 해석: {}, 자살위험: {}",
                savedResponse.getId(), userId, totalScore, interpretation, suicideRisk);

        return BDIResponseDto.from(savedResponse);
    }

    /**
     * 사용자의 최신 BDI 응답 조회
     */
    public BDIResponseDto getLatestResponse(Long userId) {
        log.debug("최신 BDI-II 응답 조회: 사용자 ID {}", userId);

        BDIResponse response = bdiResponseRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BDI_RESPONSE_NOT_FOUND));

        return BDIResponseDto.from(response);
    }

    /**
     * 사용자의 모든 BDI 응답 이력 조회
     */
    public List<BDIResponseDto> getAllResponses(Long userId) {
        log.debug("전체 BDI-II 응답 이력 조회: 사용자 ID {}", userId);

        List<BDIResponse> responses = bdiResponseRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return responses.stream()
                .map(BDIResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 최근 N개 BDI 응답 조회
     */
    public List<BDIResponseDto> getRecentResponses(Long userId, int limit) {
        log.debug("최근 {} 개 BDI-II 응답 조회: 사용자 ID {}", limit, userId);

        List<BDIResponse> responses = bdiResponseRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return responses.stream()
                .limit(limit)
                .map(BDIResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 자살 위험이 있는 모든 응답 조회 (관리자용)
     */
    public List<BDIResponseDto> getSuicideRiskResponses() {
        log.info("자살 위험 응답 목록 조회 (관리자)");

        List<BDIResponse> responses = bdiResponseRepository.findBySuicideRiskTrueOrderByCreatedAtDesc();
        return responses.stream()
                .map(BDIResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 자살 위험 응답 개수 조회
     */
    public long getSuicideRiskCount() {
        long count = bdiResponseRepository.countBySuicideRiskTrue();
        log.debug("자살 위험 응답 총 개수: {}", count);
        return count;
    }

    /**
     * 점수 기반 해석 생성
     */
    private String generateInterpretation(int totalScore) {
        if (totalScore >= 0 && totalScore <= 13) {
            return "우울감 최소/없음";
        } else if (totalScore <= 19) {
            return "경도 우울";
        } else if (totalScore <= 28) {
            return "중등도 우울";
        } else if (totalScore <= 63) {
            return "중증 우울";
        }
        throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                "BDI-II 총점이 유효 범위(0-63)를 벗어났습니다: " + totalScore);
    }
}
