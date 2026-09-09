package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.PSQIResponseDto;
import com.sleepwell.sleepwell_backend.dto.PSQIResponseRequestDto;
import com.sleepwell.sleepwell_backend.entity.PSQIResponse;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.repository.PSQIResponseRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PSQI 설문 서비스
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class PSQIService {

    private final PSQIResponseRepository psqiResponseRepository;
    private final UserRepository userRepository;

    /**
     * PSQI 설문 응답 제출
     */
    @Transactional
    public PSQIResponseDto submitResponse(Long userId, PSQIResponseRequestDto requestDto) {
        log.info("PSQI 설문 응답 제출 시작: 사용자 ID {}", userId);

        // DTO 유효성 검증
        requestDto.validateDataIntegrity();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Entity 생성 (빌더 패턴 사용)
        PSQIResponse response = PSQIResponse.builder()
                .user(user)
                .q1Bedtime(requestDto.getQ1Bedtime())
                .q2H(requestDto.getQ2H())
                .q2M(requestDto.getQ2M())
                .q3Waketime(requestDto.getQ3Waketime())
                .q4H(requestDto.getQ4H())
                .q4M(requestDto.getQ4M())
                .q5a(requestDto.getQ5a())
                .q5b(requestDto.getQ5b())
                .q5c(requestDto.getQ5c())
                .q5d(requestDto.getQ5d())
                .q5e(requestDto.getQ5e())
                .q5f(requestDto.getQ5f())
                .q5g(requestDto.getQ5g())
                .q5h(requestDto.getQ5h())
                .q5i(requestDto.getQ5i())
                .q5j(requestDto.getQ5j())
                .q5jReason(requestDto.getQ5jReason())
                .q6(requestDto.getQ6())
                .q7(requestDto.getQ7())
                .q8(requestDto.getQ8())
                .q9(requestDto.getQ9())
                .q10a(requestDto.getQ10a())
                .q10b(requestDto.getQ10b())
                .q10c(requestDto.getQ10c())
                .q10d(requestDto.getQ10d())
                .q10e(requestDto.getQ10e())
                .q10eDetail(requestDto.getQ10eDetail())
                // 7개 Component 계산
                .component1Quality(calculateComponent1(requestDto))
                .component2Latency(calculateComponent2(requestDto))
                .component3Duration(calculateComponent3(requestDto))
                .component4Efficiency(calculateComponent4(requestDto))
                .component5Disturbance(calculateComponent5(requestDto))
                .component6Medication(requestDto.getQ7())
                .component7Dysfunction(calculateComponent7(requestDto))
                .totalScore(0) // 임시값, 아래에서 계산
                .interpretation("")  // 임시값, 아래에서 계산
                .build();

        // 총점 및 해석 계산
        int totalScore = response.calculateTotalScore();
        String interpretation = response.generateInterpretation();

        // 빌더로 다시 생성 (totalScore, interpretation 설정)
        response = PSQIResponse.builder()
                .user(user)
                .q1Bedtime(requestDto.getQ1Bedtime())
                .q2H(requestDto.getQ2H())
                .q2M(requestDto.getQ2M())
                .q3Waketime(requestDto.getQ3Waketime())
                .q4H(requestDto.getQ4H())
                .q4M(requestDto.getQ4M())
                .q5a(requestDto.getQ5a())
                .q5b(requestDto.getQ5b())
                .q5c(requestDto.getQ5c())
                .q5d(requestDto.getQ5d())
                .q5e(requestDto.getQ5e())
                .q5f(requestDto.getQ5f())
                .q5g(requestDto.getQ5g())
                .q5h(requestDto.getQ5h())
                .q5i(requestDto.getQ5i())
                .q5j(requestDto.getQ5j())
                .q5jReason(requestDto.getQ5jReason())
                .q6(requestDto.getQ6())
                .q7(requestDto.getQ7())
                .q8(requestDto.getQ8())
                .q9(requestDto.getQ9())
                .q10a(requestDto.getQ10a())
                .q10b(requestDto.getQ10b())
                .q10c(requestDto.getQ10c())
                .q10d(requestDto.getQ10d())
                .q10e(requestDto.getQ10e())
                .q10eDetail(requestDto.getQ10eDetail())
                .component1Quality(response.getComponent1Quality())
                .component2Latency(response.getComponent2Latency())
                .component3Duration(response.getComponent3Duration())
                .component4Efficiency(response.getComponent4Efficiency())
                .component5Disturbance(response.getComponent5Disturbance())
                .component6Medication(response.getComponent6Medication())
                .component7Dysfunction(response.getComponent7Dysfunction())
                .totalScore(totalScore)
                .interpretation(interpretation)
                .build();

        response.validateResponses();
        PSQIResponse savedResponse = psqiResponseRepository.save(response);

        log.info("PSQI 설문 응답 저장 완료: 응답 ID {}, 사용자 ID {}, 총점 {}, 해석: {}",
                savedResponse.getId(), userId, totalScore, interpretation);

        return PSQIResponseDto.from(savedResponse);
    }

    /**
     * 사용자의 최신 PSQI 응답 조회
     */
    public PSQIResponseDto getLatestResponse(Long userId) {
        log.debug("최신 PSQI 응답 조회: 사용자 ID {}", userId);

        PSQIResponse response = psqiResponseRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PSQI_RESPONSE_NOT_FOUND));

        return PSQIResponseDto.from(response);
    }

    /**
     * 사용자의 모든 PSQI 응답 이력 조회
     */
    public List<PSQIResponseDto> getAllResponses(Long userId) {
        log.debug("전체 PSQI 응답 이력 조회: 사용자 ID {}", userId);

        List<PSQIResponse> responses = psqiResponseRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return responses.stream()
                .map(PSQIResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 최근 N개 PSQI 응답 조회
     */
    public List<PSQIResponseDto> getRecentResponses(Long userId, int limit) {
        log.debug("최근 {} 개 PSQI 응답 조회: 사용자 ID {}", limit, userId);

        List<PSQIResponse> responses = psqiResponseRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return responses.stream()
                .limit(limit)
                .map(PSQIResponseDto::from)
                .collect(Collectors.toList());
    }

    // ===== Component 계산 메서드들 =====

    private Integer calculateComponent1(PSQIResponseRequestDto dto) {
        return dto.getQ6();
    }

    private Integer calculateComponent2(PSQIResponseRequestDto dto) {
        int q2Minutes = (dto.getQ2H() * 60) + dto.getQ2M();
        int q2Score = 0;
        if (q2Minutes <= 15) q2Score = 0;
        else if (q2Minutes <= 30) q2Score = 1;
        else if (q2Minutes <= 60) q2Score = 2;
        else q2Score = 3;

        int sum = q2Score + dto.getQ5a();
        if (sum == 0) return 0;
        else if (sum <= 2) return 1;
        else if (sum <= 4) return 2;
        else return 3;
    }

    private Integer calculateComponent3(PSQIResponseRequestDto dto) {
        int totalMinutes = (dto.getQ4H() * 60) + dto.getQ4M();
        if (totalMinutes > 420) return 0;
        else if (totalMinutes >= 360) return 1;
        else if (totalMinutes >= 300) return 2;
        else return 3;
    }

    private Integer calculateComponent4(PSQIResponseRequestDto dto) {
        try {
            // 시간 파싱
            String[] bedtimeParts = dto.getQ1Bedtime().split(":");
            String[] waketimeParts = dto.getQ3Waketime().split(":");

            int bedtimeMinutes = Integer.parseInt(bedtimeParts[0]) * 60 + Integer.parseInt(bedtimeParts[1]);
            int waketimeMinutes = Integer.parseInt(waketimeParts[0]) * 60 + Integer.parseInt(waketimeParts[1]);

            long minutesInBed = waketimeMinutes - bedtimeMinutes;
            if (minutesInBed < 0) {
                minutesInBed += 24 * 60;
            }

            int actualSleepMinutes = (dto.getQ4H() * 60) + dto.getQ4M();
            double efficiency = (actualSleepMinutes / (double) minutesInBed) * 100;

            if (efficiency >= 85) return 0;
            else if (efficiency >= 75) return 1;
            else if (efficiency >= 65) return 2;
            else return 3;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                    "PSQI 수면 효율 계산 실패: 잘못된 시간 형식");
        }
    }

    private Integer calculateComponent5(PSQIResponseRequestDto dto) {
        int sum = dto.getQ5b() + dto.getQ5c() + dto.getQ5d() + dto.getQ5e() +
                  dto.getQ5f() + dto.getQ5g() + dto.getQ5h() + dto.getQ5i() + dto.getQ5j();
        if (sum == 0) return 0;
        else if (sum <= 9) return 1;
        else if (sum <= 18) return 2;
        else return 3;
    }

    private Integer calculateComponent7(PSQIResponseRequestDto dto) {
        int sum = dto.getQ8() + dto.getQ9();
        if (sum == 0) return 0;
        else if (sum <= 2) return 1;
        else if (sum <= 4) return 2;
        else return 3;
    }
}
