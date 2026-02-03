package com.dailyonepage.backend.domain.habit.service;

import com.dailyonepage.backend.domain.habit.dto.HabitLogCreateRequest;
import com.dailyonepage.backend.domain.habit.dto.HabitLogListResponse;
import com.dailyonepage.backend.domain.habit.dto.HabitLogResponse;
import com.dailyonepage.backend.domain.habit.entity.HabitLog;
import com.dailyonepage.backend.domain.habit.entity.UserHabit;
import com.dailyonepage.backend.domain.habit.repository.HabitLogRepository;
import com.dailyonepage.backend.domain.habit.repository.UserHabitRepository;
import com.dailyonepage.backend.global.exception.BusinessException;
import com.dailyonepage.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 습관 체크 기록 서비스
 *
 * 습관 체크, 조회, 취소 로직 처리
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HabitLogService {

    private final HabitLogRepository habitLogRepository;
    private final UserHabitRepository userHabitRepository;

    /**
     * 습관 체크
     */
    @Transactional
    public HabitLogResponse checkHabit(Long userId, HabitLogCreateRequest request) {
        // UserHabit 조회
        UserHabit userHabit = userHabitRepository.findById(request.getUserHabitId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_HABIT_NOT_FOUND));

        // 본인 습관인지 확인
        if (!userHabit.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        LocalDate date = request.getDateOrToday();
        boolean checked = request.isCheckedOrDefault();

        // 이미 해당 날짜에 기록이 있는지 확인
        if (habitLogRepository.findByUserHabitIdAndDate(request.getUserHabitId(), date).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_HABIT_LOG);
        }

        // HabitLog 생성
        HabitLog habitLog = HabitLog.builder()
                .userHabit(userHabit)
                .date(date)
                .checked(checked)
                .build();

        HabitLog savedLog = habitLogRepository.save(habitLog);

        // 스트릭 업데이트 (체크된 경우만)
        if (checked) {
            userHabit.checkHabit(date);
        }

        log.info("습관 체크: userId={}, userHabitId={}, date={}, checked={}",
                userId, userHabit.getId(), date, checked);

        return HabitLogResponse.from(savedLog);
    }

    /**
     * 특정 날짜의 습관 기록 조회
     * 등록된 모든 습관 + 해당 날짜 체크 여부 반환
     */
    public HabitLogListResponse getLogsByDate(Long userId, LocalDate date) {
        // 1. 사용자가 등록한 모든 습관 조회
        List<UserHabit> userHabits = userHabitRepository.findByUserIdWithHabit(userId);

        // 2. 해당 날짜의 체크 기록 조회
        List<HabitLog> logs = habitLogRepository.findByUserIdAndDate(userId, date);

        // 3. 체크 기록을 Map으로 변환 (userHabitId -> HabitLog)
        java.util.Map<Long, HabitLog> logMap = logs.stream()
                .collect(java.util.stream.Collectors.toMap(
                        log -> log.getUserHabit().getId(),
                        log -> log
                ));

        // 4. 모든 습관에 대해 응답 생성 (체크 여부 포함)
        List<HabitLogResponse> responses = userHabits.stream()
                .map(userHabit -> {
                    HabitLog log = logMap.get(userHabit.getId());
                    if (log != null) {
                        return HabitLogResponse.from(log);
                    } else {
                        return HabitLogResponse.fromUserHabitUnchecked(userHabit, date);
                    }
                })
                .toList();

        return HabitLogListResponse.of(date, responses);
    }

    /**
     * 습관 체크 취소
     */
    @Transactional
    public void cancelCheck(Long userId, Long habitLogId) {
        HabitLog habitLog = habitLogRepository.findById(habitLogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HABIT_LOG_NOT_FOUND));

        // 본인 기록인지 확인
        if (!habitLog.getUserHabit().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        UserHabit userHabit = habitLog.getUserHabit();
        LocalDate logDate = habitLog.getDate();

        // 로그 삭제
        habitLogRepository.delete(habitLog);

        // 스트릭 재계산 (오늘 체크 취소 시)
        if (logDate.equals(LocalDate.now()) || logDate.equals(userHabit.getLastCheckedDate())) {
            recalculateStreak(userHabit);
        }

        log.info("습관 체크 취소: userId={}, habitLogId={}, date={}", userId, habitLogId, logDate);
    }

    /**
     * 스트릭 재계산
     */
    private void recalculateStreak(UserHabit userHabit) {
        List<HabitLog> checkedLogs = habitLogRepository
                .findCheckedLogsByUserHabitIdOrderByDateDesc(userHabit.getId());

        if (checkedLogs.isEmpty()) {
            userHabit.resetStreak();
            return;
        }

        // 연속된 날짜 계산
        int streak = 0;
        LocalDate expectedDate = LocalDate.now();

        for (HabitLog log : checkedLogs) {
            if (log.getDate().equals(expectedDate) || log.getDate().equals(expectedDate.minusDays(1))) {
                streak++;
                expectedDate = log.getDate().minusDays(1);
            } else {
                break;
            }
        }

        LocalDate lastDate = checkedLogs.isEmpty() ? null : checkedLogs.get(0).getDate();
        userHabit.recalculateStreak(streak, lastDate);
    }
}
