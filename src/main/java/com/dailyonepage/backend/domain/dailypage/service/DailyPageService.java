package com.dailyonepage.backend.domain.dailypage.service;

import com.dailyonepage.backend.domain.dailypage.dto.*;
import com.dailyonepage.backend.domain.dailypage.entity.DailyPage;
import com.dailyonepage.backend.domain.dailypage.repository.DailyPageRepository;
import com.dailyonepage.backend.domain.user.entity.User;
import com.dailyonepage.backend.domain.user.repository.UserRepository;
import com.dailyonepage.backend.global.exception.BusinessException;
import com.dailyonepage.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 데일리 페이지 서비스
 *
 * 하루 한 장 CRUD 로직 처리
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DailyPageService {

    private final DailyPageRepository dailyPageRepository;
    private final UserRepository userRepository;

    /**
     * 페이지 작성
     */
    @Transactional
    public DailyPageResponse createPage(Long userId, DailyPageCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDate date = request.getDateOrToday();

        // 중복 체크
        if (dailyPageRepository.existsByUserIdAndDate(userId, date)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAGE);
        }

        DailyPage dailyPage = DailyPage.builder()
                .user(user)
                .date(date)
                .content(request.getContent())
                .build();

        DailyPage savedPage = dailyPageRepository.save(dailyPage);
        log.info("페이지 작성: userId={}, date={}", userId, date);

        return DailyPageResponse.from(savedPage);
    }

    /**
     * 특정 날짜 페이지 조회
     */
    public DailyPageResponse getPageByDate(Long userId, LocalDate date) {
        DailyPage dailyPage = dailyPageRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAGE_NOT_FOUND));

        return DailyPageResponse.from(dailyPage);
    }

    /**
     * 월별 캘린더 조회
     */
    public CalendarResponse getCalendar(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<DailyPage> pages = dailyPageRepository.findByUserIdAndMonth(userId, startDate, endDate);

        List<CalendarResponse.CalendarDay> days = pages.stream()
                .map(page -> CalendarResponse.CalendarDay.from(
                        page.getId(),
                        page.getDate(),
                        page.getContent()))
                .toList();

        return CalendarResponse.of(year, month, days);
    }

    /**
     * 페이지 수정
     */
    @Transactional
    public DailyPageResponse updatePage(Long userId, Long pageId, DailyPageUpdateRequest request) {
        DailyPage dailyPage = dailyPageRepository.findById(pageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAGE_NOT_FOUND));

        // 본인 페이지인지 확인
        if (!dailyPage.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        dailyPage.updateContent(request.getContent());
        log.info("페이지 수정: userId={}, pageId={}", userId, pageId);

        return DailyPageResponse.from(dailyPage);
    }

    /**
     * 페이지 삭제
     */
    @Transactional
    public void deletePage(Long userId, Long pageId) {
        DailyPage dailyPage = dailyPageRepository.findById(pageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAGE_NOT_FOUND));

        // 본인 페이지인지 확인
        if (!dailyPage.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        dailyPageRepository.delete(dailyPage);
        log.info("페이지 삭제: userId={}, pageId={}", userId, pageId);
    }
}
