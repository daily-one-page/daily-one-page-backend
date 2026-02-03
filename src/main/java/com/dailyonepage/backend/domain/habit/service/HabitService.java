package com.dailyonepage.backend.domain.habit.service;

import com.dailyonepage.backend.domain.badge.entity.BadgeSet;
import com.dailyonepage.backend.domain.badge.repository.BadgeSetRepository;
import com.dailyonepage.backend.domain.habit.dto.*;
import com.dailyonepage.backend.domain.habit.entity.Habit;
import com.dailyonepage.backend.domain.habit.repository.HabitRepository;
import com.dailyonepage.backend.domain.user.entity.User;
import com.dailyonepage.backend.domain.user.repository.UserRepository;
import com.dailyonepage.backend.global.exception.BusinessException;
import com.dailyonepage.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 습관 서비스
 *
 * 시스템 습관 조회 및 커스텀 습관 CRUD
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;
    private final BadgeSetRepository badgeSetRepository;
    private final UserRepository userRepository;

    /**
     * 시스템 습관 목록 조회 (뱃지세트 미리보기 포함)
     */
    public SystemHabitListResponse getSystemHabits() {
        List<Habit> systemHabits = habitRepository.findSystemHabits();

        List<SystemHabitResponse> responses = systemHabits.stream()
                .map(habit -> {
                    // 해당 습관에 연결된 뱃지세트 조회 (시스템 뱃지세트만)
                    List<BadgeSet> badgeSets = badgeSetRepository.findSystemBadgeSetsByHabitId(habit.getId());
                    return SystemHabitResponse.of(habit, badgeSets);
                })
                .toList();

        return SystemHabitListResponse.from(responses);
    }

    /**
     * 커스텀 습관 목록 조회 (본인 것만)
     */
    public SystemHabitListResponse getCustomHabits(Long userId) {
        List<Habit> customHabits = habitRepository.findByUserId(userId);

        List<SystemHabitResponse> responses = customHabits.stream()
                .map(habit -> SystemHabitResponse.of(habit, List.of()))
                .toList();

        return SystemHabitListResponse.from(responses);
    }

    /**
     * 커스텀 습관 생성
     */
    @Transactional
    public HabitResponse createCustomHabit(Long userId, HabitCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Habit habit = Habit.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .type(request.getType())
                .build();

        Habit savedHabit = habitRepository.save(habit);
        log.info("커스텀 습관 생성: userId={}, habitId={}, name={}", userId, savedHabit.getId(), savedHabit.getName());

        return HabitResponse.from(savedHabit);
    }

    /**
     * 커스텀 습관 수정
     */
    @Transactional
    public HabitResponse updateCustomHabit(Long userId, Long habitId, HabitUpdateRequest request) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HABIT_NOT_FOUND));

        // 시스템 습관은 수정 불가
        if (habit.isSystemHabit()) {
            throw new BusinessException(ErrorCode.SYSTEM_HABIT_NOT_MODIFIABLE);
        }

        // 본인 소유 확인
        if (!habit.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.HABIT_NOT_OWNED);
        }

        habit.update(request.getName(), request.getDescription(), request.getIcon(), request.getType());
        log.info("커스텀 습관 수정: userId={}, habitId={}, name={}", userId, habitId, request.getName());

        return HabitResponse.from(habit);
    }

    /**
     * 커스텀 습관 삭제
     */
    @Transactional
    public void deleteCustomHabit(Long userId, Long habitId) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HABIT_NOT_FOUND));

        // 시스템 습관은 삭제 불가
        if (habit.isSystemHabit()) {
            throw new BusinessException(ErrorCode.SYSTEM_HABIT_NOT_MODIFIABLE);
        }

        // 본인 소유 확인
        if (!habit.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.HABIT_NOT_OWNED);
        }

        habitRepository.delete(habit);
        log.info("커스텀 습관 삭제: userId={}, habitId={}", userId, habitId);
    }
}
