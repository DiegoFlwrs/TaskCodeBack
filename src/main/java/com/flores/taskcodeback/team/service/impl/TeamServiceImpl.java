package com.flores.taskcodeback.team.service.impl;

import com.flores.taskcodeback.config.CacheInvalidationService;
import com.flores.taskcodeback.config.CacheNames;
import com.flores.taskcodeback.application.repository.AppRepository;
import com.flores.taskcodeback.email.service.EmailService;
import com.flores.taskcodeback.exception.BadRequestException;
import com.flores.taskcodeback.exception.ResourceNotFoundException;
import com.flores.taskcodeback.task.repository.TaskRepository;
import com.flores.taskcodeback.ticket.repository.TicketRepository;
import com.flores.taskcodeback.team.dto.*;
import com.flores.taskcodeback.team.entity.Team;
import com.flores.taskcodeback.team.entity.TeamMember;
import com.flores.taskcodeback.team.repository.TeamMemberRepository;
import com.flores.taskcodeback.team.repository.TeamRepository;
import com.flores.taskcodeback.team.service.TeamService;
import com.flores.taskcodeback.user.entity.User;
import com.flores.taskcodeback.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
    private final CacheInvalidationService cacheInvalidationService;

    @Override
    @Cacheable(value = CacheNames.TEAMS, key = "#email")
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
        TeamDto result = toDto(teamRepository.save(team));
        cacheInvalidationService.evictTeams(email);
        return result;
    }

    @Override
    public TeamDto updateTeam(String email, UUID id, TeamRequestDto request) {
        User user = getUser(email);
        Team team = getTeamForUser(id, user.getId());
        if (request.getNombre() != null) team.setNombre(request.getNombre());
        if (request.getDescripcion() != null) team.setDescripcion(request.getDescripcion());
        TeamDto result = toDto(teamRepository.save(team));
        cacheInvalidationService.evictTeams(email);
        return result;
    }

    @Override
    public void deleteTeam(String email, UUID id) {
        User user = getUser(email);
        Team team = getTeamForUser(id, user.getId());
        teamRepository.delete(team);
        cacheInvalidationService.evictTeams(email);
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

            if (teamMemberRepository.existsByTeamIdAndUserId(teamId, existingUser.getId())) {
                throw new BadRequestException("El usuario ya es miembro de este equipo");
            }

            TeamMember member = TeamMember.builder()
                    .team(team)
                    .nombre(existingUser.getNombre())
                    .email(existingUser.getEmail())
                    .userId(existingUser.getId())
                    .role(request.getRole() != null ? request.getRole() : TeamMember.MemberRole.DEVELOPER)
                    .status(request.getStatus() != null ? request.getStatus() : TeamMember.MemberStatus.ACTIVO)
                    .build();

            return invalidateMemberChange(email, toMemberDto(teamMemberRepository.save(member)));
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
            if (request.getPassword().length() < 6) {
                throw new BadRequestException("La contraseña debe tener al menos 6 caracteres");
            }
            if (request.getPassword().length() > 128) {
                throw new BadRequestException("La contraseña no puede superar 128 caracteres");
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

        return invalidateMemberChange(email, toMemberDto(saved));
    }

    private TeamMemberDto invalidateMemberChange(String email, TeamMemberDto dto) {
        cacheInvalidationService.evictTeams(email);
        cacheInvalidationService.evictAllTeamMembers();
        return dto;
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

        boolean hasUpdate = request.getNombre() != null || request.getEmail() != null
                || request.getRole() != null || request.getStatus() != null;
        if (!hasUpdate) {
            throw new BadRequestException("Debes enviar al menos un campo para actualizar");
        }

        if (request.getNombre() != null) member.setNombre(request.getNombre());
        if (request.getEmail() != null) {
            if (request.getEmail().isBlank()) {
                throw new BadRequestException("El email no puede estar vacío");
            }
            userRepository.findByEmail(request.getEmail())
                    .filter(existing -> member.getUserId() == null || !existing.getId().equals(member.getUserId()))
                    .ifPresent(existing -> {
                        throw new BadRequestException("Ya existe una cuenta con el email: " + request.getEmail());
                    });
            member.setEmail(request.getEmail());
        }
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

        return invalidateMemberChange(email, toMemberDto(teamMemberRepository.save(member)));
    }

    @Override
    public DeleteMemberResultDto deleteMember(String email, UUID teamId, UUID memberId) {
        User user = getUser(email);
        getTeamForUser(teamId, user.getId());

        TeamMember member = teamMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Miembro no encontrado"));
        if (!member.getTeam().getId().equals(teamId)) {
            throw new AccessDeniedException("El miembro no pertenece a este equipo");
        }

        Long userId = member.getUserId();
        teamMemberRepository.delete(member);
        cacheInvalidationService.evictTeams(email);
        cacheInvalidationService.evictAllTeamMembers();

        if (userId == null) {
            return DeleteMemberResultDto.builder()
                    .accountDeleted(false)
                    .message("Miembro removido del equipo")
                    .build();
        }

        List<TeamMember> remainingMemberships = teamMemberRepository.findByUserId(userId);
        if (!remainingMemberships.isEmpty()) {
            log.info("Usuario id={} removido del equipo {}. Sigue en {} equipo(s)",
                    userId, teamId, remainingMemberships.size());
            return DeleteMemberResultDto.builder()
                    .accountDeleted(false)
                    .message("Miembro removido del equipo. Sigue activo en otros equipos.")
                    .build();
        }

        log.info("Eliminando cuenta y datos del usuario id={} (sin otros equipos)", userId);

        taskRepository.deleteAll(taskRepository.findByUserIdOrderByFechaDescCreatedAtDesc(userId));
        ticketRepository.deleteAll(ticketRepository.findByUserIdOrderByCreatedAtDesc(userId));
        appRepository.deleteAll(appRepository.findByUserIdOrderByNombreAsc(userId));
        userRepository.clearCreatedBy(userId);
        userRepository.deleteById(userId);

        return DeleteMemberResultDto.builder()
                .accountDeleted(true)
                .message("Miembro y cuenta eliminados permanentemente")
                .build();
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
                .ownerId(team.getOwner() != null ? team.getOwner().getId() : null)
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
