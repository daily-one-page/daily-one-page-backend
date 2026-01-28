package com.dailyonepage.backend.domain.dailypage.entity;

import com.dailyonepage.backend.domain.user.entity.User;
import com.dailyonepage.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 데일리 페이지
 *
 * 매일 한 페이지씩 기록하는 일기/노트
 * 사용자당 날짜별로 하나의 페이지만 존재
 */
@Entity
@Table(name = "daily_page",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyPage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public DailyPage(User user, LocalDate date, String content) {
        this.user = user;
        this.date = date;
        this.content = content;
    }

    /**
     * 내용 수정
     */
    public void updateContent(String content) {
        this.content = content;
    }
}
