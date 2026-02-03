package com.dailyonepage.backend.domain.habit.service;

import com.dailyonepage.backend.domain.habit.dto.UserHabitCreateRequest;
import com.dailyonepage.backend.domain.habit.dto.UserHabitDetailResponse;
import com.dailyonepage.backend.domain.habit.dto.UserHabitListResponse;
import com.dailyonepage.backend.domain.habit.dto.UserHabitResponse;
import com.dailyonepage.backend.domain.habit.entity.Habit;
import com.dailyonepage.backend.domain.habit.entity.UserHabit;
import com.dailyonepage.backend.domain.habit.repository.HabitRepository;
import com.dailyonepage.backend.domain.habit.repository.UserHabitRepository;
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
 * 사용자 습관 서비스
 *
 * 사용자가 습관을 등록/해제하는 로직 처리
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserHabitService {

    private final UserHabitRepository userHabitRepository;
    private final HabitRepository habitRepository;
    private final UserRepository userRepository;

    /**
     * 내 습관 목록 조회
     */
    public UserHabitListResponse getMyHabits(Long userId) {
        List<UserHabit> userHabits = userHabitRepository.findByUserIdWithHabit(userId);

        List<UserHabitResponse> responses = userHabits.stream()
                .map(UserHabitResponse::from)
                .toList();

        return UserHabitListResponse.from(responses);
    }

    /**
     * 내 습관 상세 조회
     */
    public UserHabitDetailResponse getMyHabitDetail(Long userId, Long userHabitId) {
        UserHabit userHabit = userHabitRepository.findByIdWithHabit(userHabitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_HABIT_NOT_FOUND));

        // 본인 것만 조회 가능
        if (!userHabit.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return UserHabitDetailResponse.from(userHabit);
    }

    /**
     * 습관 등록
     */
    @Transactional
    public UserHabitResponse registerHabit(Long userId, UserHabitCreateRequest request) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 습관 조회
        Habit habit = habitRepository.findById(request.getHabitId())
                .orElseThrow(() -> new BusinessException(ErrorCode.HABIT_NOT_FOUND));

        // 중복 등록 체크
        if (userHabitRepository.existsByUserIdAndHabitId(userId, request.getHabitId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER_HABIT);
        }

        // 커스텀 습관인 경우, 본인 것만 등록 가능
        if (habit.isCustomHabit() && !habit.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.HABIT_NOT_OWNED);
        }

        // 등록
        UserHabit userHabit = UserHabit.builder()
                .user(user)
                .habit(habit)
                .build();

        UserHabit savedUserHabit = userHabitRepository.save(userHabit);
        log.info("습관 등록: userId={}, habitId={}, habitName={}", userId, habit.getId(), habit.getName());

        return UserHabitResponse.from(savedUserHabit);
    }

    /**
     * 습관 해제
     */
    @Transactional
    public void unregisterHabit(Long userId, Long userHabitId) {
        UserHabit userHabit = userHabitRepository.findById(userHabitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_HABIT_NOT_FOUND));

        // 본인 것만 해제 가능
        if (!userHabit.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        userHabitRepository.delete(userHabit);
        log.info("습관 해제: userId={}, userHabitId={}", userId, userHabitId);
    }
}
