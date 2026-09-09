package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.lifestyle.CreateLifestyleDataRequestDto;
import com.sleepwell.sleepwell_backend.dto.lifestyle.LifestyleDataResponseDto;
import com.sleepwell.sleepwell_backend.entity.LifestyleData;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.repository.LifestyleDataRepository;
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
public class LifestyleDataService {

    private final LifestyleDataRepository lifestyleDataRepository;
    private final UserRepository userRepository;

    @Transactional
    public LifestyleDataResponseDto createLifestyleData(Long userId, CreateLifestyleDataRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 사용자를 찾을 수 없습니다: " + userId));

        LifestyleData lifestyleData = LifestyleData.builder()
                .user(user)
                .recordDate(dto.getRecordDate())
                .exerciseType(dto.getExerciseType())
                .exerciseIntensity(dto.getExerciseIntensity())
                .exerciseDurationMinutes(dto.getExerciseDurationMinutes())
                .exerciseStartTime(dto.getExerciseStartTime())
                .caffeineIntake(dto.getCaffeineIntake())
                .lastCaffeineTime(dto.getLastCaffeineTime())
                .alcoholIntake(dto.getAlcoholIntake())
                .lastAlcoholTime(dto.getLastAlcoholTime())
                .stressLevel(dto.getStressLevel())
                .screenTimeMinutes(dto.getScreenTimeMinutes())
                .preBedrimeScreenTime(dto.getPreBedrimeScreenTime())
                .build();

        LifestyleData savedData = lifestyleDataRepository.save(lifestyleData);
        return LifestyleDataResponseDto.from(savedData);
    }

    public LifestyleDataResponseDto getLifestyleData(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 사용자를 찾을 수 없습니다: " + userId));

        return lifestyleDataRepository.findByUserAndRecordDate(user, date)
                .map(LifestyleDataResponseDto::from)
                .orElseThrow(() -> new EntityNotFoundException("해당 날짜의 생활 패턴 데이터를 찾을 수 없습니다: " + date));
    }

    public List<LifestyleDataResponseDto> getLifestyleDataForPeriod(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 사용자를 찾을 수 없습니다: " + userId));

        List<LifestyleData> lifestyleDataList = lifestyleDataRepository.findByUserAndRecordDateBetweenOrderByRecordDateDesc(user, startDate, endDate);

        return lifestyleDataList.stream()
                .map(LifestyleDataResponseDto::from)
                .collect(Collectors.toList());
    }
} 