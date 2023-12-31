package com.example.giftlistb8.repositories;

import com.example.giftlistb8.entities.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    @Query("select u from UserInfo u where u.resetToken = ?1")
    UserInfo findByResetPasswordToken(String token);
}