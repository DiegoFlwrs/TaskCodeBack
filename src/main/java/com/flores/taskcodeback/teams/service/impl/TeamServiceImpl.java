package com.flores.taskcodeback.teams.service.impl;

import com.flores.taskcodeback.app.repository.AppRepository;
import com.flores.taskcodeback.email.service.EmailService;
import com.flores.taskcodeback.exception.BadRequestException;
import com.flores.taskcodeback.exception.ResourceNotFoundException;
import com.flores.taskcodeback.task.repository.TaskRepository;
import com.flores.taskcodeback.ticket.repository.TicketRepository;
import com.flores.taskcodeback.teams.dto.*;
import com.flores.taskcodeback.teams.entity.Team;
import com.flores.taskcodeback.teams.entity.TeamMember;
import com.flores.taskcodeback.teams.repository.TeamMemberRepository;
import com.flores.taskcodeback.teams.repository.TeamRepository;
import com.flores.taskcodeback.teams.service.TeamService;
import com.flores.taskcodeback.user.entity.User;
import com.flores.taskcodeback.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TaskRepository taskRepository;
    private final TicketRepository ticketRepository;
    private final AppRepository appRepository;

    // ...existing getTeams, createTeam, updateTeam, deleteTeam...

    @Override
    @Transactional(readOnly = true)
    public List<TeamDto> getTeams(String email) {
        User user = getUser(email);

        // Equipos donde el usuario es owner
        List<Team> teams = new ArrayList<>(
                teamRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId()));

        // Equipos donde el usuario es miembro (via user_team_members)
        List<UUID> ownerTeamIds = teams.stream().map(Team::getId).collect(Collectors.toList());
        teamMemberRepository.findByUserId(user.getId()).stream()
                .map(TeamMember::getTeam)
                .filter(team -> !ownerTeamIds.contains(team.getId())) // evitar duplicados
                .forEach(teams::add);

        return teams.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public TeamDto createTeam(String email, TeamRequestDto request) {
        User user = getUser(email);
        String code;
        do { code = generateCode(); } while (teamRepository.existsByCodigo(code));

        Team team = Team.builder()
                .owner(user)
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .codigo(code)
                .build();
        return toDto(teamRepository.save(team));
    }

    @Override
    public TeamDto updateTeam(String email, UUID id, TeamRequestDto request) {
        User user = getUser(email);
        Team team = getTeamForUser(id, user.getId());
        if (request.getNombre() != null) team.setNombre(request.getNombre());
        if (request.getDescripcion() != null) team.setDescripcion(request.getDescripcion());
        return toDto(teamRepository.save(team));
    }

    @Override
    public void deleteTeam(String email, UUID id) {
        User user = getUser(email);
        Team team = getTeamForUser(id, user.getId());
        teamRepository.delete(team);
    }

    @Override
    public TeamMemberDto addMember(String email, UUID teamId, TeamMemberRequestDto request) {
        User leader = getUser(email);
        Team team = getTeamForUser(teamId, leader.getId());

        // ── CASO 1: asociar usuario ya existente ──────────────────────────
        if (request.getExistingUserId() != null) {
            User existingUser = userRepository.findById(request.getExistingUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Usuario no encontrado con id: " + request.getExistingUserId()));

            TeamMember member = TeamMember.builder()
                    .team(team)
                    .nombre(existingUser.getNombre())
                    .email(existingUser.getEmail())
                    .userId(existingUser.getId())
                    .role(request.getRole() != null ? request.getRole() : TeamMember.MemberRole.DEVELOPER)
                    .status(request.getStatus() != null ? request.getStatus() : TeamMember.MemberStatus.ACTIVO)
                    .build();

            return toMemberDto(teamMemberRepository.save(member));
        }

        // ── CASO 2: crear usuario nuevo ───────────────────────────────────
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new BadRequestException("El nombre es requerido para crear un usuario nuevo");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("El email es requerido para crear un usuario nuevo");
        }

        // Verificar que el email no esté ya registrado
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Ya existe una cuenta con el email: " + request.getEmail());
        }

        // Determinar contraseña
        boolean isAuto = !"manual".equalsIgnoreCase(request.getPasswordMode());
        String plainPassword;
        if (isAuto) {
            plainPassword = generateSecurePassword();
        } else {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new BadRequestException("El campo password es requerido cuando passwordMode es 'manual'");
            }
            plainPassword = request.getPassword();
        }

        // Crear cuenta de usuario para el nuevo miembro
        User newUser = User.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(passwordEncoder.encode(plainPassword))
                .role(User.Role.USER)
                .isIndependent(false)
                .activo(true)
                .emailVerified(true)
                .createdBy(leader)
                .build();
        User savedUser = userRepository.save(newUser);

        // Crear el miembro del equipo
        TeamMember member = TeamMember.builder()
                .team(team)
                .nombre(request.getNombre())
                .email(request.getEmail())
                .userId(savedUser.getId())
                .role(request.getRole() != null ? request.getRole() : TeamMember.MemberRole.DEVELOPER)
                .status(request.getStatus() != null ? request.getStatus() : TeamMember.MemberStatus.ACTIVO)
                .build();
        TeamMember saved = teamMemberRepository.save(member);

        // Enviar email de bienvenida con credenciales
        try {
            emailService.sendWelcomeMemberEmail(
                    request.getEmail(),
                    request.getNombre(),
                    team.getNombre(),
                    leader.getNombre(),
                    plainPassword
            );
        } catch (Exception ex) {
            log.warn("No se pudo enviar email de bienvenida a {}: {}", request.getEmail(), ex.getMessage());
        }

        return toMemberDto(saved);
    }

    @Override
    public TeamMemberDto updateMember(String email, UUID teamId, UUID memberId, TeamMemberRequestDto request) {
        User user = getUser(email);
        getTeamForUser(teamId, user.getId());

        TeamMember member = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Miembro no encontrado"));
        if (!member.getTeam().getId().equals(teamId)) {
            throw new AccessDeniedException("El miembro no pertenece a este equipo");
        }

        if (request.getNombre() != null) member.setNombre(request.getNombre());
        if (request.getEmail() != null) member.setEmail(request.getEmail());
        if (request.getRole() != null) member.setRole(request.getRole());
        if (request.getStatus() != null) {
            member.setStatus(request.getStatus());

            // Sincronizar activo/inactivo en la cuenta User asociada
            if (member.getUserId() != null) {
                userRepository.findById(member.getUserId()).ifPresent(memberUser -> {
                    boolean debeEstarActivo = request.getStatus() == TeamMember.MemberStatus.ACTIVO;
                    memberUser.setActivo(debeEstarActivo);
                    userRepository.save(memberUser);
                    log.info("Cuenta userId={} marcada como activo={}", member.getUserId(), debeEstarActivo);
                });
            }
        }

        return toMemberDto(teamMemberRepository.save(member));
    }

    @Override
    public void deleteMember(String email, UUID teamId, UUID memberId) {
        User user = getUser(email);
        getTeamForUser(teamId, user.getId());

        TeamMember member = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Miembro no encontrado"));
        if (!member.getTeam().getId().equals(teamId)) {
            throw new AccessDeniedException("El miembro no pertenece a este equipo");
        }

        // Eliminar cuenta de usuario y todo lo asociado a él
        if (member.getUserId() != null) {
            Long userId = member.getUserId();
            log.info("Eliminando cuenta y datos del usuario id={}", userId);

            // 1. Eliminar tareas del usuario
            taskRepository.deleteAll(taskRepository.findByUserIdOrderByFechaDescCreatedAtDesc(userId));

            // 2. Eliminar tickets del usuario
            ticketRepository.deleteAll(ticketRepository.findByUserIdOrderByCreatedAtDesc(userId));

            // 3. Eliminar aplicaciones del usuario
            appRepository.deleteAll(appRepository.findByUserIdOrderByNombreAsc(userId));

            // 4. Limpiar referencias createdBy que apunten a este usuario
            userRepository.clearCreatedBy(userId);

            // 5. Eliminar la cuenta de usuario
            userRepository.deleteById(userId);
        }

        // Eliminar el miembro del equipo
        teamMemberRepository.delete(member);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String generateSecurePassword() {
        String upper   = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower   = "abcdefghjkmnpqrstuvwxyz";
        String digits  = "23456789";
        String special = "@#$!";
        String all     = upper + lower + digits + special;
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        sb.append(upper.charAt(rnd.nextInt(upper.length())));
        sb.append(lower.charAt(rnd.nextInt(lower.length())));
        sb.append(digits.charAt(rnd.nextInt(digits.length())));
        sb.append(special.charAt(rnd.nextInt(special.length())));
        for (int i = 4; i < 12; i++) sb.append(all.charAt(rnd.nextInt(all.length())));
        // mezclar
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            char tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp;
        }
        return new String(chars);
    }

    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) code.append(chars.charAt(random.nextInt(chars.length())));
        return code.toString();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Team getTeamForUser(UUID id, Long userId) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado"));
        if (!team.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("No tienes permisos para acceder a este equipo");
        }
        return team;
    }

    private TeamDto toDto(Team team) {
        List<TeamMemberDto> members = team.getMembers() != null
                ? team.getMembers().stream().map(this::toMemberDto).collect(Collectors.toList())
                : List.of();
        return TeamDto.builder()
                .id(team.getId())
                .nombre(team.getNombre())
                .descripcion(team.getDescripcion())
                .codigo(team.getCodigo())
                .members(members)
                .createdAt(team.getCreatedAt())
                .build();
    }

    private TeamMemberDto toMemberDto(TeamMember member) {
        return TeamMemberDto.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .nombre(member.getNombre())
                .email(member.getEmail())
                .role(member.getRole())
                .status(member.getStatus())
                .build();
    }
}
