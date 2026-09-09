package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.SISResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SIS 설문 응답 레포지토리
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Repository
public interface SISResponseRepository extends JpaRepository<SISResponse, Long> {

    /**
     * 사용자 ID로 SIS 응답 목록 조회 (최신순)
     */
    List<SISResponse> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자의 최신 SIS 응답 조회
     */
    Optional<SISResponse> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
