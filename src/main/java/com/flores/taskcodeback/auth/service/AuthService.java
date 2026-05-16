package com.flores.taskcodeback.auth.service;

import com.flores.taskcodeback.auth.dto.*;
import com.flores.taskcodeback.user.entity.User;

public interface AuthService {

    AuthResponseDto registerIndependent(RegisterRequestDto request);

    AuthResponseDto registerTeamLeader(RegisterTeamLeaderRequestDto request);

    // Nuevos métodos para verificación
    AuthResponseDto registerIndependentVerified(RegisterRequestDto request);

    AuthResponseDto registerTeamLeaderVerified(RegisterTeamLeaderRequestDto request);

    boolean existsByEmail(String email);

    AuthResponseDto login(LoginRequestDto request);

    void logout(String token);

    String generateTeamCode();

    User getCurrentUser();
}
