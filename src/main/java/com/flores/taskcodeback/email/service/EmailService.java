package com.flores.taskcodeback.email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.verification.from}")
    private String fromEmail;

    public void sendVerificationCode(String toEmail, String verificationCode, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("TaskCode - Código de verificación");
            message.setText(buildVerificationEmailText(userName, verificationCode));

            mailSender.send(message);
            log.info("Código de verificación enviado exitosamente a: {}", toEmail);

        } catch (Exception e) {
            log.error("Error enviando código de verificación a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Error enviando email de verificación", e);
        }
    }

    private String buildVerificationEmailText(String userName, String verificationCode) {
        return String.format("""
            Hola %s,
            
            ¡Bienvenido a TaskCode!
            
            Tu código de verificación es: %s
            
            Este código expira en 5 minutos.
            
            Si no solicitaste esta verificación, ignora este email.
            
            Saludos,
            El equipo de TaskCodeBack
            """, userName, verificationCode);
    }
}
