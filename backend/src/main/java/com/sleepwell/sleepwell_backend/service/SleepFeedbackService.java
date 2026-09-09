package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.feedback.CreateSleepFeedbackRequestDto;
import com.sleepwell.sleepwell_backend.dto.feedback.SleepFeedbackResponseDto;
import com.sleepwell.sleepwell_backend.entity.SleepFeedback;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.repository.SleepFeedbackRepository;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SleepFeedbackService {

    private final SleepFeedbackRepository sleepFeedbackRepository;
    private final UserRepository userRepository;
    private final SleepRecordRepository sleepRecordRepository;

    @Transactional
    public SleepFeedbackResponseDto createSleepFeedback(Long userId, CreateSleepFeedbackRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 사용자를 찾을 수 없습니다: " + userId));

        SleepRecord sleepRecord = sleepRecordRepository.findById(dto.getSleepRecordId())
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 수면 기록을 찾을 수 없습니다: " + dto.getSleepRecordId()));
        
        // 해당 수면 기록이 사용자의 소유인지 확인하는 로직 추가 (보안 강화)
        if (!sleepRecord.getUser().getId().equals(userId)) {
            throw new SecurityException("해당 수면 기록에 접근할 권한이 없습니다.");
        }

        SleepFeedback feedback = SleepFeedback.builder()
                .user(user)
                .sleepRecord(sleepRecord)
                .feedbackDate(dto.getFeedbackDate())
                .overallSatisfaction(dto.getOverallSatisfaction())
                .fatigueRecovery(dto.getFatigueRecovery())
                .morningFreshness(dto.getMorningFreshness())
                .morningCondition(dto.getMorningCondition())
                .perceivedSleepDepth(dto.getPerceivedSleepDepth())
                .perceivedWakeupCount(dto.getPerceivedWakeupCount())
                .sleepNotes(dto.getSleepNotes())
                .build();

        SleepFeedback savedFeedback = sleepFeedbackRepository.save(feedback);
        return SleepFeedbackResponseDto.from(savedFeedback);
    }

    public SleepFeedbackResponseDto getFeedbackForSleepRecord(Long userId, Long sleepRecordId) {
        SleepRecord sleepRecord = sleepRecordRepository.findById(sleepRecordId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 수면 기록을 찾을 수 없습니다: " + sleepRecordId));
        
        if (!sleepRecord.getUser().getId().equals(userId)) {
            throw new SecurityException("해당 수면 기록에 접근할 권한이 없습니다.");
        }
        
        return sleepFeedbackRepository.findBySleepRecord(sleepRecord)
                .map(SleepFeedbackResponseDto::from)
                .orElseThrow(() -> new EntityNotFoundException("해당 수면 기록에 대한 피드백을 찾을 수 없습니다."));
    }

    public List<SleepFeedbackResponseDto> getFeedbacksForPeriod(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 사용자를 찾을 수 없습니다: " + userId));

        List<SleepFeedback> feedbacks = sleepFeedbackRepository.findByUserAndFeedbackDateBetweenOrderByFeedbackDateDesc(user, startDate, endDate);

        return feedbacks.stream()
                .map(SleepFeedbackResponseDto::from)
                .collect(Collectors.toList());
    }
} 