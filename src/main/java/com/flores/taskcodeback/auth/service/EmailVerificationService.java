package com.flores.taskcodeback.auth.service;

import com.flores.taskcodeback.auth.entity.EmailVerificationCode;
import com.flores.taskcodeback.auth.repository.EmailVerificationCodeRepository;
import com.flores.taskcodeback.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmailVerificationService {

    private static final String CODE_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;

    @Value("${app.email.verification.expiration:300000}")
    private Long verificationExpirationMs;

    @Async
    public void sendVerificationCode(String email, String userName) {
        log.info("Generando código de verificación para: {}", email);

        // Invalidar códigos anteriores
        verificationCodeRepository.markAllAsUsedByEmail(email);

        // Generar nuevo código
        String codigo = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(verificationExpirationMs / 1000);

        // Guardar en BD
        EmailVerificationCode verificationCode = EmailVerificationCode.builder()
                .email(email)
                .codigo(codigo)
                .expiresAt(expiresAt)
                .build();

        verificationCodeRepository.save(verificationCode);

        // Enviar email asíncronamente
        emailService.sendVerificationCode(email, codigo, userName);
    }

    public boolean verifyCode(String email, String codigo) {
        log.info("Verificando código para email: {}", email);

        String normalizedCode = codigo == null ? null : codigo.trim().toUpperCase();

        var verificationCode = verificationCodeRepository
                .findByEmailAndCodigoAndUsedFalse(email, normalizedCode)
                .orElse(null);

        if (verificationCode == null) {
            log.warn("Código no encontrado o ya usado para: {}", email);
            return false;
        }

        if (verificationCode.isExpired()) {
            log.warn("Código expirado para: {}", email);
            return false;
        }

        // Marcar como usado
        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        log.info("Código verificado exitosamente para: {}", email);
        return true;
    }

    private String generateVerificationCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(CODE_CHARSET.length());
            code.append(CODE_CHARSET.charAt(index));
        }
        return code.toString();
    }

    @Transactional
    public void cleanupExpiredCodes() {
        log.info("Limpiando códigos de verificación expirados");
        verificationCodeRepository.deleteExpiredCodes(LocalDateTime.now());
    }
}
