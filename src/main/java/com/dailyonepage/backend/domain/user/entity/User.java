package com.dailyonepage.backend.domain.user.entity;

import com.dailyonepage.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 사용자 엔티티
 *
 * @Entity: JPA가 관리하는 엔티티 클래스
 * @Table: 테이블명 지정 (user는 예약어라서 users로 변경)
 * @NoArgsConstructor(access = PROTECTED): JPA 프록시 생성용, 외부 생성 방지
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "alarm_time")
    private LocalTime alarmTime;

    @Builder
    public User(String email, String password, String nickname, LocalTime alarmTime) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.alarmTime = alarmTime;
    }

    // 비즈니스 메서드
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateAlarmTime(LocalTime alarmTime) {
        this.alarmTime = alarmTime;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
