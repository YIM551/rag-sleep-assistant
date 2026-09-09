package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.ASMRContent;
import com.sleepwell.sleepwell_backend.enums.ASMRCategory;
import com.sleepwell.sleepwell_backend.enums.ASMRContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ASMR 콘텐츠 Repository
 */
@Repository
public interface ASMRContentRepository extends JpaRepository<ASMRContent, Long> {

    Page<ASMRContent> findByStatus(ASMRContentStatus status, Pageable pageable);

    Page<ASMRContent> findByCategoryAndStatus(ASMRCategory category, ASMRContentStatus status, Pageable pageable);

    List<ASMRContent> findByStatusAndIsActiveOrderByTotalPlayCountDesc(ASMRContentStatus status, Boolean isActive);

    Optional<ASMRContent> findByIdAndStatus(Long id, ASMRContentStatus status);

    @Query("SELECT a FROM ASMRContent a WHERE a.status = :status AND " +
           "(:category IS NULL OR a.category = :category) AND " +
           "(:targetMood IS NULL OR a.targetMood = :targetMood) AND " +
           "(:minDuration IS NULL OR a.durationMinutes >= :minDuration) AND " +
           "(:maxDuration IS NULL OR a.durationMinutes <= :maxDuration)")
    Page<ASMRContent> findBySearchCriteria(@Param("status") ASMRContentStatus status,
                                          @Param("category") ASMRCategory category,
                                          @Param("targetMood") String targetMood,
                                          @Param("minDuration") Integer minDuration,
                                          @Param("maxDuration") Integer maxDuration,
                                          Pageable pageable);

    List<ASMRContent> findTop10ByStatusOrderByTotalPlayCountDesc(ASMRContentStatus status);

    List<ASMRContent> findByTagsContainingAndStatus(String tag, ASMRContentStatus status);

    List<ASMRContent> findByIsPremiumAndStatus(Boolean isPremium, ASMRContentStatus status);
}