package com.flores.taskcodeback.auth.repository;

import com.flores.taskcodeback.auth.entity.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findByEmailAndCodigoAndUsedFalse(String email, String codigo);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerificationCode e WHERE e.expiresAt < :now")
    void deleteExpiredCodes(LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE EmailVerificationCode e SET e.used = true WHERE e.email = :email AND e.used = false")
    void markAllAsUsedByEmail(String email);

    boolean existsByEmailAndUsedFalse(String email);
}
