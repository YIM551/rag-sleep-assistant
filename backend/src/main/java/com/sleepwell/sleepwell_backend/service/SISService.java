package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.SISResponseDto;
import com.sleepwell.sleepwell_backend.dto.SISResponseRequestDto;
import com.sleepwell.sleepwell_backend.entity.SISResponse;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.repository.SISResponseRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SIS 설문 서비스
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class SISService {

    private final SISResponseRepository sisResponseRepository;
    private final UserRepository userRepository;

    /**
     * SIS 설문 응답 제출
     */
    @Transactional
    public SISResponseDto submitResponse(Long userId, SISResponseRequestDto requestDto) {
        log.info("SIS 설문 응답 제출 시작: 사용자 ID {}", userId);

        // DTO 유효성 검증
        requestDto.validateDataIntegrity();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 총점 계산
        int totalScore = requestDto.getQ1a() + requestDto.getQ1b() + requestDto.getQ1c() + requestDto.getQ1d() +
                        requestDto.getQ1e() + requestDto.getQ1f() + requestDto.getQ1g() + requestDto.getQ1h() +
                        requestDto.getQ2a() + requestDto.getQ2b() + requestDto.getQ2c() + requestDto.getQ2d() +
                        requestDto.getQ3a() + requestDto.getQ3b() + requestDto.getQ3c() + requestDto.getQ3d() + requestDto.getQ3e() +
                        requestDto.getQ4a() + requestDto.getQ4b() + requestDto.getQ4c() + requestDto.getQ4d() +
                        requestDto.getQ4e() + requestDto.getQ4f() + requestDto.getQ4g() +
                        requestDto.getQ5a() + requestDto.getQ5b() + requestDto.getQ5c() + requestDto.getQ5d() +
                        requestDto.getQ6a() + requestDto.getQ6b() + requestDto.getQ6c() + requestDto.getQ6d() +
                        requestDto.getQ7a() + requestDto.getQ7b() + requestDto.getQ7c();

        // 해석 생성
        String interpretation = generateInterpretation(totalScore);

        // Entity 생성 및 저장
        SISResponse response = SISResponse.builder()
                .user(user)
                .q1a(requestDto.getQ1a())
                .q1b(requestDto.getQ1b())
                .q1c(requestDto.getQ1c())
                .q1d(requestDto.getQ1d())
                .q1e(requestDto.getQ1e())
                .q1f(requestDto.getQ1f())
                .q1g(requestDto.getQ1g())
                .q1h(requestDto.getQ1h())
                .q2a(requestDto.getQ2a())
                .q2b(requestDto.getQ2b())
                .q2c(requestDto.getQ2c())
                .q2d(requestDto.getQ2d())
                .q3a(requestDto.getQ3a())
                .q3b(requestDto.getQ3b())
                .q3c(requestDto.getQ3c())
                .q3d(requestDto.getQ3d())
                .q3e(requestDto.getQ3e())
                .q4a(requestDto.getQ4a())
                .q4b(requestDto.getQ4b())
                .q4c(requestDto.getQ4c())
                .q4d(requestDto.getQ4d())
                .q4e(requestDto.getQ4e())
                .q4f(requestDto.getQ4f())
                .q4g(requestDto.getQ4g())
                .q5a(requestDto.getQ5a())
                .q5b(requestDto.getQ5b())
                .q5c(requestDto.getQ5c())
                .q5d(requestDto.getQ5d())
                .q6a(requestDto.getQ6a())
                .q6b(requestDto.getQ6b())
                .q6c(requestDto.getQ6c())
                .q6d(requestDto.getQ6d())
                .q7a(requestDto.getQ7a())
                .q7b(requestDto.getQ7b())
                .q7c(requestDto.getQ7c())
                .totalScore(totalScore)
                .interpretation(interpretation)
                .build();

        response.validateResponses();
        SISResponse savedResponse = sisResponseRepository.save(response);

        log.info("SIS 설문 응답 저장 완료: 응답 ID {}, 사용자 ID {}, 총점 {}, 해석: {}",
                savedResponse.getId(), userId, totalScore, interpretation);

        return SISResponseDto.from(savedResponse);
    }

    /**
     * 사용자의 최신 SIS 응답 조회
     */
    public SISResponseDto getLatestResponse(Long userId) {
        log.debug("최신 SIS 응답 조회: 사용자 ID {}", userId);

        SISResponse response = sisResponseRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SIS_RESPONSE_NOT_FOUND));

        return SISResponseDto.from(response);
    }

    /**
     * 사용자의 모든 SIS 응답 이력 조회
     */
    public List<SISResponseDto> getAllResponses(Long userId) {
        log.debug("전체 SIS 응답 이력 조회: 사용자 ID {}", userId);

        List<SISResponse> responses = sisResponseRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return responses.stream()
                .map(SISResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 최근 N개 SIS 응답 조회
     */
    public List<SISResponseDto> getRecentResponses(Long userId, int limit) {
        log.debug("최근 {} 개 SIS 응답 조회: 사용자 ID {}", limit, userId);

        List<SISResponse> responses = sisResponseRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return responses.stream()
                .limit(limit)
                .map(SISResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 점수 기반 해석 생성
     */
    private String generateInterpretation(int totalScore) {
        if (totalScore >= 35 && totalScore <= 70) {
            return "경미한 수면 영향";
        } else if (totalScore <= 105) {
            return "중등도 수면 영향";
        } else if (totalScore <= 140) {
            return "심각한 수면 영향";
        } else if (totalScore <= 175) {
            return "매우 심각한 수면 영향";
        }
        throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                "SIS 총점이 유효 범위(35-175)를 벗어났습니다: " + totalScore);
    }
}
