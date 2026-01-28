package com.dailyonepage.backend.domain.ai.entity;

import com.dailyonepage.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 피드백
 *
 * 사용자가 접속 시 어제 기록을 기반으로 AI가 생성한 피드백
 * 하루에 피드백 1개만 생성 (재생성 불가)
 */
@Entity
@Table(name = "ai_feedback",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 피드백 대상 날짜 (어제 기록에 대한 피드백)
     */
    @Column(nullable = false)
    private LocalDate date;

    /**
     * AI가 생성한 피드백 메시지
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AiFeedback(User user, LocalDate date, String message) {
        this.user = user;
        this.date = date;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }
}
