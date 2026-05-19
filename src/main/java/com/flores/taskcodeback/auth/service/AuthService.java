package com.flores.taskcodeback.auth.service;

import com.flores.taskcodeback.auth.dto.*;

public interface AuthService {

    void sendPasswordResetCode(ForgotPasswordRequestDto request);

    void resetPasswordWithCode(ResetPasswordWithCodeRequestDto request);

    // Registro actual basado en verificacion de email
    AuthResponseDto registerIndependentVerified(RegisterRequestDto request);

    AuthResponseDto registerTeamLeaderVerified(RegisterTeamLeaderRequestDto request);

    boolean existsByEmail(String email);

    AuthResponseDto login(LoginRequestDto request);

    String generateTeamCode();
}
