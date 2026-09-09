package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.ISIResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ISI 설문 응답 레포지토리
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Repository
public interface ISIResponseRepository extends JpaRepository<ISIResponse, Long> {

    /**
     * 사용자 ID로 ISI 응답 조회
     */
    Optional<ISIResponse> findByUserId(Long userId);

    /**
     * 사용자 ID로 ISI 응답 목록 조회 (최신순)
     */
    List<ISIResponse> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자의 ISI 응답 존재 여부 확인
     */
    boolean existsByUserId(Long userId);

    /**
     * 사용자의 최신 ISI 응답 조회
     */
    Optional<ISIResponse> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
