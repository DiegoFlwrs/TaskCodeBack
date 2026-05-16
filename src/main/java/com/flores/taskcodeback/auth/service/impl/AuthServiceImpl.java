package com.flores.taskcodeback.auth.service.impl;

import com.flores.taskcodeback.auth.dto.*;
import com.flores.taskcodeback.auth.service.AuthService;
import com.flores.taskcodeback.auth.service.EmailVerificationService;
import com.flores.taskcodeback.config.JwtConfig;
import com.flores.taskcodeback.equipo.entity.Equipo;
import com.flores.taskcodeback.equipo.repository.EquipoRepository;
import com.flores.taskcodeback.exception.BadRequestException;
import com.flores.taskcodeback.exception.EmailAlreadyExistsException;
import com.flores.taskcodeback.security.JwtTokenProvider;
import com.flores.taskcodeback.user.dto.UserDto;
import com.flores.taskcodeback.user.entity.User;
import com.flores.taskcodeback.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EquipoRepository equipoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final JwtConfig jwtConfig;
    private final EmailVerificationService emailVerificationService;

    @Override
    public void sendPasswordResetCode(ForgotPasswordRequestDto request) {
        log.info("Solicitud de recuperacion de contrasena para: {}", request.getEmail());

        userRepository.findByEmail(request.getEmail())
                .ifPresentOrElse(
                        user -> emailVerificationService.sendVerificationCode(user.getEmail(), user.getNombre()),
                        () -> log.warn("Se solicito recuperacion para un email no registrado: {}", request.getEmail())
                );
    }

    @Override
    public void resetPasswordWithCode(ResetPasswordWithCodeRequestDto request) {
        log.info("Intentando restablecer contrasena para: {}", request.getEmail());

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("La confirmacion de contrasena no coincide");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No existe una cuenta asociada a este email"));

        if (!emailVerificationService.verifyCode(request.getEmail(), request.getVerificationCode())) {
            throw new BadRequestException("Codigo de verificacion invalido o expirado");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("La nueva contrasena no puede ser igual a la actual");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Contrasena restablecida correctamente para: {}", request.getEmail());
    }

    @Override
    public AuthResponseDto registerIndependent(RegisterRequestDto request) {
        log.info("Registrando usuario independiente: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Ya existe un usuario con el email: " + request.getEmail());
        }

        User user = User.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .isIndependent(true)
                .equipo(null)
                .createdBy(null)
                .activo(true)
                .emailVerified(true) // Por simplicidad, auto-verificamos
                .build();

        User savedUser = userRepository.save(user);
        log.info("Usuario independiente registrado exitosamente: {}", savedUser.getEmail());

        // Generar token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        String token = tokenProvider.generateToken(authentication);

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration())
                .user(mapToUserDto(savedUser))
                .build();
    }

    @Override
    public AuthResponseDto registerTeamLeader(RegisterTeamLeaderRequestDto request) {
        log.info("Registrando líder de equipo: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Ya existe un usuario con el email: " + request.getEmail());
        }

        // Generar código único para el equipo
        String teamCode;
        do {
            teamCode = generateTeamCode();
        } while (equipoRepository.existsByCodigo(teamCode));

        // Crear equipo
        Equipo equipo = Equipo.builder()
                .nombre(request.getEquipoNombre())
                .descripcion(request.getEquipoDescripcion())
                .codigo(teamCode)
                .activo(true)
                .build();

        // Crear líder
        User leader = User.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.TEAM_LEADER)
                .isIndependent(false)
                .equipo(equipo)
                .createdBy(null)
                .activo(true)
                .emailVerified(true)
                .build();

        // Asignar líder al equipo
        equipo.setLeader(leader);

        // Guardar equipo (cascada guarda el líder)
        Equipo savedEquipo = equipoRepository.save(equipo);
        User savedLeader = savedEquipo.getLeader();

        log.info("Líder de equipo registrado exitosamente: {} con equipo: {}",
                savedLeader.getEmail(), savedEquipo.getNombre());

        // Generar token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        String token = tokenProvider.generateToken(authentication);

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration())
                .user(mapToUserDto(savedLeader))
                .build();
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        log.info("Intento de login para: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        // Actualizar último login
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = tokenProvider.generateToken(authentication);

        log.info("Login exitoso para: {}", request.getEmail());

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration())
                .user(mapToUserDto(user))
                .build();
    }

    @Override
    public void logout(String token) {
        // Por ahora, el logout es del lado del cliente
        // En el futuro se puede implementar blacklist de tokens
        log.info("Logout realizado");
    }

    @Override
    public String generateTeamCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Usuario no autenticado");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
    }

    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .nombre(user.getNombre())
                .email(user.getEmail())
                .role(user.getRole())
                .isIndependent(user.getIsIndependent())
                .activo(user.getActivo())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    @Override
    public AuthResponseDto registerIndependentVerified(RegisterRequestDto request) {
        log.info("Registrando usuario independiente verificado: {}", request.getEmail());

        // No verificamos email duplicado aquí porque ya fue verificado en el paso anterior
        User user = User.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .isIndependent(true)
                .equipo(null)
                .createdBy(null)
                .activo(true)
                .emailVerified(true) // Email ya verificado
                .build();

        User savedUser = userRepository.save(user);
        log.info("Usuario independiente verificado registrado exitosamente: {}", savedUser.getEmail());

        // Generar token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        String token = tokenProvider.generateToken(authentication);

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration())
                .user(mapToUserDto(savedUser))
                .build();
    }

    @Override
    public AuthResponseDto registerTeamLeaderVerified(RegisterTeamLeaderRequestDto request) {
        log.info("Registrando líder de equipo verificado: {}", request.getEmail());

        // Generar código único para el equipo
        String teamCode;
        do {
            teamCode = generateTeamCode();
        } while (equipoRepository.existsByCodigo(teamCode));

        // Crear equipo
        Equipo equipo = Equipo.builder()
                .nombre(request.getEquipoNombre())
                .descripcion(request.getEquipoDescripcion())
                .codigo(teamCode)
                .activo(true)
                .build();

        // Crear líder
        User leader = User.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.TEAM_LEADER)
                .isIndependent(false)
                .equipo(equipo)
                .createdBy(null)
                .activo(true)
                .emailVerified(true) // Email ya verificado
                .build();

        // Asignar líder al equipo
        equipo.setLeader(leader);

        // Guardar equipo (cascada guarda el líder)
        Equipo savedEquipo = equipoRepository.save(equipo);
        User savedLeader = savedEquipo.getLeader();

        log.info("Líder de equipo verificado registrado exitosamente: {} con equipo: {}",
                savedLeader.getEmail(), savedEquipo.getNombre());

        // Generar token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        String token = tokenProvider.generateToken(authentication);

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration())
                .user(mapToUserDto(savedLeader))
                .build();
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
