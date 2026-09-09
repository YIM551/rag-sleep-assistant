package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.BDIResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BDI-II 설문 응답 레포지토리
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Repository
public interface BDIResponseRepository extends JpaRepository<BDIResponse, Long> {

    /**
     * 사용자 ID로 BDI 응답 조회
     */
    Optional<BDIResponse> findByUserId(Long userId);

    /**
     * 사용자 ID로 BDI 응답 목록 조회 (최신순)
     */
    List<BDIResponse> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자의 BDI 응답 존재 여부 확인
     */
    boolean existsByUserId(Long userId);

    /**
     * 사용자의 최신 BDI 응답 조회
     */
    Optional<BDIResponse> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 자살 위험이 있는 모든 응답 조회 (관리자 모니터링용)
     * 최신순 정렬
     */
    List<BDIResponse> findBySuicideRiskTrueOrderByCreatedAtDesc();

    /**
     * 자살 위험이 있는 응답 개수 조회
     */
    long countBySuicideRiskTrue();
}
