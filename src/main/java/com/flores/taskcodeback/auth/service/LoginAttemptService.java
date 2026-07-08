package com.flores.taskcodeback.auth.service;

import com.flores.taskcodeback.config.SecurityProperties;
import com.flores.taskcodeback.exception.BadRequestException;
import com.flores.taskcodeback.user.entity.User;
import com.flores.taskcodeback.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoginAttemptService {

    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;

    public void assertNotLocked(User user) {
        if (isCurrentlyLocked(user)) {
            throw new BadRequestException(getLockMessage(user));
        }
    }

    public boolean isCurrentlyLocked(User user) {
        if (user.getLockedUntil() == null) {
            return false;
        }
        if (LocalDateTime.now().isBefore(user.getLockedUntil())) {
            return true;
        }
        resetFailedAttempts(user);
        userRepository.save(user);
        return false;
    }

    public void onSuccessfulLogin(User user) {
        resetFailedAttempts(user);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    public void onFailedLogin(User user) {
        int current = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
        int attempts = current + 1;
        user.setFailedLoginAttempts(attempts);

        int maxAttempts = securityProperties.getMaxLoginAttempts();

        if (attempts >= maxAttempts) {
            user.setLockedUntil(LocalDateTime.now()
                    .plusMinutes(securityProperties.getLockoutDurationMinutes()));
            userRepository.save(user);
            log.warn("Cuenta bloqueada tras {} intentos fallidos: {}", attempts, user.getEmail());
            throw new BadRequestException(getLockMessage(user));
        }

        userRepository.save(user);
        int remaining = maxAttempts - attempts;
        throw new BadRequestException(
                "Email o contraseña incorrectos. Te quedan " + remaining + " intento"
                        + (remaining == 1 ? "" : "s") + ".");
    }

    public void resetFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
    }

    public String getLockMessage(User user) {
        if (user.getLockedUntil() == null) {
            return "Cuenta bloqueada temporalmente por múltiples intentos fallidos. "
                    + "Intenta de nuevo más tarde.";
        }

        long minutes = Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes();
        long displayMinutes = Math.max(1, minutes);

        return "Cuenta bloqueada tras " + securityProperties.getMaxLoginAttempts()
                + " intentos fallidos. Intenta de nuevo en " + displayMinutes + " minuto"
                + (displayMinutes == 1 ? "" : "s") + ".";
    }
}
