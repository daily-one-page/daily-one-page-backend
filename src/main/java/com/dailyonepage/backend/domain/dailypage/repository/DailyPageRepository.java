package com.dailyonepage.backend.domain.dailypage.repository;

import com.dailyonepage.backend.domain.dailypage.entity.DailyPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPageRepository extends JpaRepository<DailyPage, Long> {

    // 사용자의 특정 날짜 페이지 조회
    Optional<DailyPage> findByUserIdAndDate(Long userId, LocalDate date);

    // 사용자의 월별 페이지 목록 (캘린더용)
    @Query("SELECT dp FROM DailyPage dp WHERE dp.user.id = :userId " +
            "AND dp.date BETWEEN :startDate AND :endDate ORDER BY dp.date ASC")
    List<DailyPage> findByUserIdAndMonth(@Param("userId") Long userId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    // 페이지 존재 여부 (빠른 체크용)
    boolean existsByUserIdAndDate(Long userId, LocalDate date);

    // 사용자의 총 페이지 수
    long countByUserId(Long userId);

    // 사용자의 최근 페이지
    @Query("SELECT dp FROM DailyPage dp WHERE dp.user.id = :userId ORDER BY dp.date DESC LIMIT 1")
    Optional<DailyPage> findLatestByUserId(@Param("userId") Long userId);
}
