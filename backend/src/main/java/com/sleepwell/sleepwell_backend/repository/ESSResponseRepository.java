package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.ESSResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ESS 설문 응답 레포지토리
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Repository
public interface ESSResponseRepository extends JpaRepository<ESSResponse, Long> {

    /**
     * 사용자 ID로 ESS 응답 조회
     */
    Optional<ESSResponse> findByUserId(Long userId);

    /**
     * 사용자 ID로 ESS 응답 목록 조회 (최신순)
     */
    List<ESSResponse> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자의 ESS 응답 존재 여부 확인
     */
    boolean existsByUserId(Long userId);

    /**
     * 사용자의 최신 ESS 응답 조회
     */
    Optional<ESSResponse> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
