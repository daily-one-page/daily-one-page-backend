package com.dailyonepage.backend.domain.ai.repository;

import com.dailyonepage.backend.domain.ai.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    // 사용자의 특정 날짜 피드백 조회
    Optional<AiFeedback> findByUserIdAndDate(Long userId, LocalDate date);

    // 피드백 존재 여부 (중복 생성 방지용)
    boolean existsByUserIdAndDate(Long userId, LocalDate date);

    // 사용자의 월별 피드백 목록
    @Query("SELECT af FROM AiFeedback af WHERE af.user.id = :userId " +
            "AND af.date BETWEEN :startDate AND :endDate ORDER BY af.date ASC")
    List<AiFeedback> findByUserIdAndMonth(@Param("userId") Long userId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    // 사용자의 최근 피드백
    @Query("SELECT af FROM AiFeedback af WHERE af.user.id = :userId ORDER BY af.date DESC LIMIT 1")
    Optional<AiFeedback> findLatestByUserId(@Param("userId") Long userId);
}
