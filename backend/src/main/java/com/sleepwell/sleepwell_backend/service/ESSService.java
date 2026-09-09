package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.ESSResponseDto;
import com.sleepwell.sleepwell_backend.dto.ESSResponseRequestDto;
import com.sleepwell.sleepwell_backend.entity.ESSResponse;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.repository.ESSResponseRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ESS 설문 서비스
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class ESSService {

    private final ESSResponseRepository essResponseRepository;
    private final UserRepository userRepository;

    /**
     * ESS 설문 응답 제출
     */
    @Transactional
    public ESSResponseDto submitResponse(Long userId, ESSResponseRequestDto requestDto) {
        log.info("ESS 설문 응답 제출 시작: 사용자 ID {}", userId);

        // DTO 유효성 검증
        requestDto.validateDataIntegrity();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 총점 및 해석 계산
        int totalScore = requestDto.getQ1() + requestDto.getQ2() + requestDto.getQ3() + requestDto.getQ4() +
                        requestDto.getQ5() + requestDto.getQ6() + requestDto.getQ7() + requestDto.getQ8();
        String interpretation = generateInterpretation(totalScore);

        // Entity 생성 및 저장
        ESSResponse response = ESSResponse.builder()
                .user(user)
                .q1(requestDto.getQ1())
                .q2(requestDto.getQ2())
                .q3(requestDto.getQ3())
                .q4(requestDto.getQ4())
                .q5(requestDto.getQ5())
                .q6(requestDto.getQ6())
                .q7(requestDto.getQ7())
                .q8(requestDto.getQ8())
                .totalScore(totalScore)
                .interpretation(interpretation)
                .build();

        response.validateResponses();
        ESSResponse savedResponse = essResponseRepository.save(response);

        log.info("ESS 설문 응답 저장 완료: 응답 ID {}, 사용자 ID {}, 총점 {}, 해석: {}",
                savedResponse.getId(), userId, totalScore, interpretation);

        return ESSResponseDto.from(savedResponse);
    }

    /**
     * 사용자의 최신 ESS 응답 조회
     */
    public ESSResponseDto getLatestResponse(Long userId) {
        log.debug("최신 ESS 응답 조회: 사용자 ID {}", userId);

        ESSResponse response = essResponseRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESS_RESPONSE_NOT_FOUND));

        return ESSResponseDto.from(response);
    }

    /**
     * 사용자의 모든 ESS 응답 이력 조회
     */
    public List<ESSResponseDto> getAllResponses(Long userId) {
        log.debug("전체 ESS 응답 이력 조회: 사용자 ID {}", userId);

        List<ESSResponse> responses = essResponseRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return responses.stream()
                .map(ESSResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 최근 N개 ESS 응답 조회
     */
    public List<ESSResponseDto> getRecentResponses(Long userId, int limit) {
        log.debug("최근 {} 개 ESS 응답 조회: 사용자 ID {}", limit, userId);

        List<ESSResponse> responses = essResponseRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return responses.stream()
                .limit(limit)
                .map(ESSResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 점수 기반 해석 생성
     */
    private String generateInterpretation(int totalScore) {
        if (totalScore >= 0 && totalScore <= 10) {
            return "정상 범위";
        } else if (totalScore <= 15) {
            return "경도 졸림증";
        } else if (totalScore <= 24) {
            return "중등도 이상 졸림증";
        }
        throw new BusinessException(ErrorCode.INVALID_SURVEY_SCORE_RANGE,
                "ESS 총점이 유효 범위(0-24)를 벗어났습니다: " + totalScore);
    }
}
