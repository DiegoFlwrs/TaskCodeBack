package com.flores.taskcodeback.auth.controller;

import com.flores.taskcodeback.auth.dto.*;
import com.flores.taskcodeback.auth.service.AuthService;
import com.flores.taskcodeback.auth.service.EmailVerificationService;
import com.flores.taskcodeback.user.dto.UserDto;
import com.flores.taskcodeback.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/forgot-password/send-code")
    public ResponseEntity<Map<String, String>> sendPasswordResetCode(@Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.sendPasswordResetCode(request);
        // Mensaje neutro para no revelar si el email existe o no.
        return ResponseEntity.ok(Map.of(
                "message", "Si el correo existe, se envio un codigo de recuperacion"
        ));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, String>> resetPasswordWithCode(@Valid @RequestBody ResetPasswordWithCodeRequestDto request) {
        authService.resetPasswordWithCode(request);
        return ResponseEntity.ok(Map.of("message", "Contrasena actualizada correctamente"));
    }

    // 🔥 NUEVO: Enviar código de verificación
    @PostMapping("/send-verification-code")
    public ResponseEntity<Map<String, String>> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequestDto request) {
        log.info("Solicitud de código de verificación para: {}", request.getEmail());

        // Verificar que el email no esté ya registrado
        if (authService.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Email ya registrado",
                "message", "Ya existe una cuenta con este email"
            ));
        }

        emailVerificationService.sendVerificationCode(request.getEmail(), request.getNombre());

        return ResponseEntity.ok(Map.of(
            "message", "Código de verificación enviado",
            "email", request.getEmail()
        ));
    }

    // Verificar código y registrar usuario independiente
    @PostMapping("/verify-and-register")
    public ResponseEntity<AuthResponseDto> verifyAndRegisterIndependent(@Valid @RequestBody VerifyCodeAndRegisterRequestDto request) {
        log.info("Verificación y registro independiente para: {}", request.getEmail());

        // Verificar código
        if (!emailVerificationService.verifyCode(request.getEmail(), request.getVerificationCode())) {
            return ResponseEntity.badRequest().build();
        }

        // Proceder con registro normal
        RegisterRequestDto registerRequest = RegisterRequestDto.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

        AuthResponseDto response = authService.registerIndependentVerified(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Verificar código y registrar team leader
    @PostMapping("/verify-and-register-team-leader")
    public ResponseEntity<AuthResponseDto> verifyAndRegisterTeamLeader(@Valid @RequestBody VerifyCodeAndRegisterTeamLeaderRequestDto request) {
        log.info("Verificación y registro team leader para: {}", request.getEmail());

        // Verificar código
        if (!emailVerificationService.verifyCode(request.getEmail(), request.getVerificationCode())) {
            return ResponseEntity.badRequest().build();
        }

        // Proceder con registro de team leader
        RegisterTeamLeaderRequestDto registerRequest = RegisterTeamLeaderRequestDto.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(request.getPassword())
                .equipoNombre(request.getEquipoNombre())
                .equipoDescripcion(request.getEquipoDescripcion())
                .build();

        AuthResponseDto response = authService.registerTeamLeaderVerified(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("Solicitud de login recibida para: {}", request.getEmail());
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }



    @GetMapping("/generate-team-code")
    public ResponseEntity<String> generateTeamCode() {
        String code = authService.generateTeamCode();
        return ResponseEntity.ok(code);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal User user) {
        UserDto dto = UserDto.builder()
                .id(user.getId())
                .nombre(user.getNombre())
                .email(user.getEmail())
                .role(user.getRole())
                .isIndependent(user.getIsIndependent())
                .activo(user.getActivo())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
        return ResponseEntity.ok(dto);
    }
}

