package com.dailyonepage.backend.domain.ai.service;

import com.dailyonepage.backend.domain.ai.dto.AiFeedbackHistoryResponse;
import com.dailyonepage.backend.domain.ai.dto.AiFeedbackResponse;
import com.dailyonepage.backend.domain.ai.entity.AiFeedback;
import com.dailyonepage.backend.domain.ai.repository.AiFeedbackRepository;
import com.dailyonepage.backend.domain.dailypage.entity.DailyPage;
import com.dailyonepage.backend.domain.dailypage.repository.DailyPageRepository;
import com.dailyonepage.backend.domain.habit.entity.HabitLog;
import com.dailyonepage.backend.domain.habit.repository.HabitLogRepository;
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
import java.util.Optional;

/**
 * AI 피드백 서비스
 *
 * AI 피드백 생성 및 조회 로직 처리
 * (현재는 Mock AI 사용, 추후 실제 AI API 연동)
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AiFeedbackService {

    private final AiFeedbackRepository aiFeedbackRepository;
    private final UserRepository userRepository;
    private final HabitLogRepository habitLogRepository;
    private final DailyPageRepository dailyPageRepository;

    /**
     * 오늘의 피드백 조회 (없으면 생성)
     */
    @Transactional
    public AiFeedbackResponse getTodayFeedback(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 이미 오늘 피드백이 있으면 반환
        Optional<AiFeedback> existingFeedback = aiFeedbackRepository.findByUserIdAndDate(userId, today);
        if (existingFeedback.isPresent()) {
            return AiFeedbackResponse.from(existingFeedback.get());
        }

        // 어제 데이터 조회
        List<HabitLog> yesterdayLogs = habitLogRepository.findByUserIdAndDate(userId, yesterday);
        Optional<DailyPage> yesterdayPage = dailyPageRepository.findByUserIdAndDate(userId, yesterday);

        // 어제 데이터가 없으면 피드백 생성 불가
        if (yesterdayLogs.isEmpty() && yesterdayPage.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_DATA_FOR_FEEDBACK);
        }

        // AI 피드백 생성 (Mock)
        String message = generateMockFeedback(yesterdayLogs, yesterdayPage.orElse(null));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        AiFeedback feedback = AiFeedback.builder()
                .user(user)
                .date(today)
                .message(message)
                .build();

        AiFeedback savedFeedback = aiFeedbackRepository.save(feedback);
        log.info("AI 피드백 생성: userId={}, date={}", userId, today);

        return AiFeedbackResponse.from(savedFeedback);
    }

    /**
     * 특정 날짜 피드백 조회
     */
    public AiFeedbackResponse getFeedbackByDate(Long userId, LocalDate date) {
        AiFeedback feedback = aiFeedbackRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDBACK_NOT_FOUND));

        return AiFeedbackResponse.from(feedback);
    }

    /**
     * 월별 피드백 히스토리 조회
     */
    public AiFeedbackHistoryResponse getFeedbackHistory(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<AiFeedback> feedbacks = aiFeedbackRepository.findByUserIdAndMonth(userId, startDate, endDate);

        List<AiFeedbackResponse> responses = feedbacks.stream()
                .map(AiFeedbackResponse::from)
                .toList();

        return AiFeedbackHistoryResponse.of(year, month, responses);
    }

    /**
     * 최근 피드백 조회
     */
    public Optional<AiFeedbackResponse> getLatestFeedback(Long userId) {
        return aiFeedbackRepository.findLatestByUserId(userId)
                .map(AiFeedbackResponse::from);
    }

    /**
     * Mock AI 피드백 생성
     * (추후 실제 AI API로 대체)
     */
    private String generateMockFeedback(List<HabitLog> habitLogs, DailyPage dailyPage) {
        StringBuilder sb = new StringBuilder();

        // 습관 체크 기반 피드백
        if (!habitLogs.isEmpty()) {
            long checkedCount = habitLogs.stream().filter(HabitLog::isChecked).count();
            sb.append(String.format("어제 %d개의 습관을 체크하셨네요! ", checkedCount));

            if (checkedCount == habitLogs.size()) {
                sb.append("모든 습관을 완료하셨습니다. 대단해요! 🎉 ");
            } else {
                sb.append("꾸준함이 중요합니다. 오늘도 화이팅! 💪 ");
            }
        }

        // 페이지 작성 기반 피드백
        if (dailyPage != null) {
            int contentLength = dailyPage.getContent().length();
            if (contentLength > 200) {
                sb.append("어제 정성스럽게 기록을 남기셨네요. ");
            }
            sb.append("매일 기록하는 습관이 쌓이고 있습니다. ✍️");
        }

        if (sb.isEmpty()) {
            sb.append("오늘도 좋은 하루 되세요! 🌟");
        }

        return sb.toString();
    }
}
