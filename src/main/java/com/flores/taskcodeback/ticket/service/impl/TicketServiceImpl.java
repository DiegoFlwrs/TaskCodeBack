package com.flores.taskcodeback.ticket.service.impl;

import com.flores.taskcodeback.config.CacheInvalidationService;
import com.flores.taskcodeback.config.CacheNames;
import com.flores.taskcodeback.exception.BadRequestException;
import com.flores.taskcodeback.exception.ResourceNotFoundException;
import com.flores.taskcodeback.team.entity.TeamMember;
import com.flores.taskcodeback.team.entity.Team;
import com.flores.taskcodeback.team.repository.TeamMemberRepository;
import com.flores.taskcodeback.team.repository.TeamRepository;
import com.flores.taskcodeback.ticket.dto.*;
import com.flores.taskcodeback.ticket.entity.Ticket;
import com.flores.taskcodeback.ticket.repository.TicketRepository;
import com.flores.taskcodeback.ticket.service.TicketService;
import com.flores.taskcodeback.user.entity.User;
import com.flores.taskcodeback.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CacheInvalidationService cacheInvalidationService;

    @Override
    @Cacheable(value = CacheNames.TICKETS, key = "#email + ':' + @cacheKeyBuilder.ticketKey(#status != null ? #status.name() : null)")
    @Transactional(readOnly = true)
    public List<TicketDto> getTickets(String email, Ticket.TicketStatus status) {
        User user = getUser(email);

        List<Ticket> owned = status != null
                ? ticketRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status)
                : ticketRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<UUID> memberIds = teamMemberRepository.findByUserId(user.getId()).stream()
                .map(TeamMember::getId)
                .collect(Collectors.toList());

        List<Ticket> assigned = memberIds.isEmpty()
                ? Collections.emptyList()
                : (status != null
                    ? ticketRepository.findByAssignedMemberIdInAndStatus(memberIds, status)
                    : ticketRepository.findByAssignedMemberIdIn(memberIds));

        LinkedHashSet<UUID> ticketIds = new LinkedHashSet<>();
        owned.forEach(t -> ticketIds.add(t.getId()));
        assigned.forEach(t -> ticketIds.add(t.getId()));

        if (ticketIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Ticket> tickets = ticketRepository.findAllByIdInWithAssignments(new ArrayList<>(ticketIds));
        tickets.sort(Comparator.comparing(Ticket::getCreatedAt).reversed());

        return tickets.stream().map(t -> toDto(t, user)).collect(Collectors.toList());
    }

    @Override
    public TicketDto createTicket(String email, TicketRequestDto request) {
        User user = getUser(email);
        validateTicketRequest(request, user, null);
        validateUniqueNombre(request.getTeamId(), user.getId(), request.getNombre(), null);
        validateUniqueCodigo(request.getTeamId(), user.getId(), request.getCodigo(), null);

        Ticket ticket = Ticket.builder()
                .user(user)
                .teamId(request.getTeamId())
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .asignadoPor(request.getAsignadoPor())
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .priority(request.getPriority() != null ? request.getPriority() : Ticket.TicketPriority.MEDIA)
                .status(request.getStatus() != null ? request.getStatus() : Ticket.TicketStatus.ACTIVO)
                .build();

        ticket = ticketRepository.save(ticket);

        if (request.getAssignedMemberIds() != null) {
            syncAssignedMembers(ticket, request.getAssignedMemberIds(), user);
            ticket = ticketRepository.save(ticket);
        }

        return invalidateAndReturn(email, toDto(ticket, user));
    }

    @Override
    public TicketDto updateTicket(String email, UUID id, TicketRequestDto request) {
        User user = getUser(email);
        Ticket ticket = getTicketForManagement(id, user);

        UUID teamId = request.getTeamId() != null ? request.getTeamId() : ticket.getTeamId();
        String nombre = request.getNombre() != null ? request.getNombre() : ticket.getNombre();
        String codigo = request.getCodigo() != null ? request.getCodigo() : ticket.getCodigo();
        LocalDate fechaInicio = request.getFechaInicio() != null ? request.getFechaInicio() : ticket.getFechaInicio();
        LocalDate fechaFin = request.getFechaFin() != null ? request.getFechaFin() : ticket.getFechaFin();

        validateDateRange(fechaInicio, fechaFin);
        validateTeamAccess(teamId, user);
        validateUniqueNombre(teamId, user.getId(), nombre, ticket.getId());
        validateUniqueCodigo(teamId, user.getId(), codigo, ticket.getId());

        if (request.getCodigo() != null) ticket.setCodigo(request.getCodigo());
        if (request.getNombre() != null) ticket.setNombre(request.getNombre());
        if (request.getDescripcion() != null) ticket.setDescripcion(request.getDescripcion());
        if (request.getAsignadoPor() != null) ticket.setAsignadoPor(request.getAsignadoPor());
        if (request.getTeamId() != null) ticket.setTeamId(request.getTeamId());
        if (request.getFechaInicio() != null) ticket.setFechaInicio(request.getFechaInicio());
        if (request.getFechaFin() != null) ticket.setFechaFin(request.getFechaFin());
        if (request.getPriority() != null) ticket.setPriority(request.getPriority());
        if (request.getStatus() != null) ticket.setStatus(request.getStatus());
        if (request.getMotivo() != null) ticket.setMotivo(request.getMotivo());

        if (request.getAssignedMemberIds() != null) {
            syncAssignedMembers(ticket, request.getAssignedMemberIds(), user);
        }

        return invalidateAndReturn(email, toDto(ticketRepository.save(ticket), user));
    }

    @Override
    public void deleteTicket(String email, UUID id) {
        User user = getUser(email);
        Ticket ticket = getTicketForManagement(id, user);
        ticketRepository.delete(ticket);
        cacheInvalidationService.evictTickets(email);
    }

    @Override
    public TicketDto requestExtension(String email, UUID id, TicketExtensionRequestDto request) {
        User user = getUser(email);
        Ticket ticket = getTicketById(id);

        if (!isAssignedToUser(ticket, user)) {
            throw new AccessDeniedException("Solo los miembros asignados pueden solicitar una extensión");
        }
        if (canManageTicket(ticket, user)) {
            throw new BadRequestException("Como líder de equipo, extiende la fecha directamente");
        }
        if (Boolean.TRUE.equals(ticket.getExtensionPendiente())) {
            throw new BadRequestException("Ya existe una solicitud de extensión pendiente de aprobación");
        }
        if (!request.getFechaFin().isAfter(ticket.getFechaFin())) {
            throw new BadRequestException("La nueva fecha debe ser posterior a la fecha de fin actual");
        }

        ticket.setExtensionPendiente(true);
        ticket.setExtensionFechaSolicitada(request.getFechaFin());
        ticket.setExtensionMotivo(request.getMotivo());
        ticket.setExtensionSolicitadaPorUserId(user.getId());

        return invalidateAndReturn(email, toDto(ticketRepository.save(ticket), user));
    }

    @Override
    public TicketDto reviewExtension(String email, UUID id, TicketExtensionReviewDto review) {
        User user = getUser(email);
        Ticket ticket = getTicketById(id);

        if (!canManageTicket(ticket, user)) {
            throw new AccessDeniedException("Solo el líder de equipo puede aprobar o rechazar extensiones");
        }
        if (!Boolean.TRUE.equals(ticket.getExtensionPendiente())) {
            throw new BadRequestException("No hay solicitud de extensión pendiente");
        }

        if (Boolean.TRUE.equals(review.getApproved())) {
            ticket.setFechaFin(ticket.getExtensionFechaSolicitada());
            ticket.setMotivo(ticket.getExtensionMotivo());
        }

        clearExtensionRequest(ticket);
        return invalidateAndReturn(email, toDto(ticketRepository.save(ticket), user));
    }

    private TicketDto invalidateAndReturn(String email, TicketDto dto) {
        cacheInvalidationService.evictTickets(email);
        return dto;
    }

    private void validateTicketRequest(TicketRequestDto request, User user, UUID excludeId) {
        validateDateRange(request.getFechaInicio(), request.getFechaFin());
        validateTeamAccess(request.getTeamId(), user);
    }

    private void validateDateRange(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return;
        }
        if (fechaFin.isBefore(fechaInicio)) {
            throw new BadRequestException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
    }

    private void validateTeamAccess(UUID teamId, User user) {
        if (teamId == null) {
            return;
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BadRequestException("El equipo especificado no existe"));

        boolean isOwner = team.getOwner().getId().equals(user.getId());
        boolean isMember = teamMemberRepository.existsByTeamIdAndUserId(teamId, user.getId());

        if (!isOwner && !isMember) {
            throw new AccessDeniedException("No tienes acceso al equipo especificado");
        }
    }

    private void validateUniqueCodigo(UUID teamId, Long userId, String codigo, UUID excludeId) {
        if (codigo == null || codigo.isBlank()) {
            return;
        }

        String normalized = codigo.trim();
        boolean exists;

        if (teamId != null) {
            exists = excludeId == null
                    ? ticketRepository.existsByTeamIdAndCodigoIgnoreCase(teamId, normalized)
                    : ticketRepository.existsByTeamIdAndCodigoIgnoreCaseAndIdNot(teamId, normalized, excludeId);
        } else {
            exists = excludeId == null
                    ? ticketRepository.existsByUserIdAndTeamIdIsNullAndCodigoIgnoreCase(userId, normalized)
                    : ticketRepository.existsByUserIdAndTeamIdIsNullAndCodigoIgnoreCaseAndIdNot(userId, normalized, excludeId);
        }

        if (exists) {
            throw new BadRequestException("Ya existe un ticket con el código \"" + normalized + "\"");
        }
    }

    private void validateUniqueNombre(UUID teamId, Long userId, String nombre, UUID excludeId) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }

        String normalized = nombre.trim();
        boolean exists;

        if (teamId != null) {
            exists = excludeId == null
                    ? ticketRepository.existsByTeamIdAndNombreIgnoreCase(teamId, normalized)
                    : ticketRepository.existsByTeamIdAndNombreIgnoreCaseAndIdNot(teamId, normalized, excludeId);
        } else {
            exists = excludeId == null
                    ? ticketRepository.existsByUserIdAndTeamIdIsNullAndNombreIgnoreCase(userId, normalized)
                    : ticketRepository.existsByUserIdAndTeamIdIsNullAndNombreIgnoreCaseAndIdNot(userId, normalized, excludeId);
        }

        if (exists) {
            throw new BadRequestException("Ya existe un ticket con el nombre \"" + normalized + "\"");
        }
    }

    private void syncAssignedMembers(Ticket ticket, List<UUID> memberIds, User user) {
        if (user.getRole() != User.Role.TEAM_LEADER) {
            if (!memberIds.isEmpty()) {
                throw new AccessDeniedException("Solo el líder de equipo puede asignar miembros al ticket");
            }
            return;
        }

        if (ticket.getTeamId() == null) {
            throw new BadRequestException("El ticket debe pertenecer a un equipo para asignar miembros");
        }

        if (memberIds.isEmpty()) {
            ticket.getAssignedMembers().clear();
            return;
        }

        List<TeamMember> members = teamMemberRepository.findAllById(memberIds);
        if (members.size() != memberIds.size()) {
            throw new BadRequestException("Uno o más miembros no existen");
        }

        for (TeamMember member : members) {
            if (!member.getTeam().getId().equals(ticket.getTeamId())) {
                throw new BadRequestException("El miembro " + member.getNombre() + " no pertenece al equipo del ticket");
            }
            if (member.getStatus() != TeamMember.MemberStatus.ACTIVO) {
                throw new BadRequestException("Solo se pueden asignar miembros activos");
            }
            if (!member.getTeam().getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException("No tienes permisos para asignar miembros de este equipo");
            }
        }

        ticket.getAssignedMembers().clear();
        ticket.getAssignedMembers().addAll(members);
    }

    private void clearExtensionRequest(Ticket ticket) {
        ticket.setExtensionPendiente(false);
        ticket.setExtensionFechaSolicitada(null);
        ticket.setExtensionMotivo(null);
        ticket.setExtensionSolicitadaPorUserId(null);
    }

    private boolean canManageTicket(Ticket ticket, User user) {
        if (ticket.getUser().getId().equals(user.getId())) {
            return true;
        }
        if (user.getRole() == User.Role.TEAM_LEADER && ticket.getTeamId() != null) {
            return teamRepository.findById(ticket.getTeamId())
                    .map(team -> team.getOwner().getId().equals(user.getId()))
                    .orElse(false);
        }
        return false;
    }

    private boolean isAssignedToUser(Ticket ticket, User user) {
        if (ticket.getAssignedMembers() == null) return false;
        return ticket.getAssignedMembers().stream()
                .anyMatch(m -> m.getUserId() != null && m.getUserId().equals(user.getId()));
    }

    private boolean isAssigneeOnly(Ticket ticket, User user) {
        return isAssignedToUser(ticket, user) && !canManageTicket(ticket, user);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Ticket getTicketById(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));
    }

    private Ticket getTicketForManagement(UUID id, User user) {
        Ticket ticket = getTicketById(id);
        if (!canManageTicket(ticket, user)) {
            throw new AccessDeniedException("No tienes permisos para modificar este ticket");
        }
        return ticket;
    }

    private TicketDto toDto(Ticket ticket, User currentUser) {
        String teamNombre = null;
        if (ticket.getTeamId() != null) {
            teamNombre = teamRepository.findById(ticket.getTeamId())
                    .map(t -> t.getNombre())
                    .orElse(null);
        }

        List<TicketAssignedMemberDto> assignedMembers = ticket.getAssignedMembers() == null
                ? Collections.emptyList()
                : ticket.getAssignedMembers().stream()
                    .map(m -> TicketAssignedMemberDto.builder()
                            .id(m.getId())
                            .nombre(m.getNombre())
                            .email(m.getEmail())
                            .build())
                    .collect(Collectors.toList());

        boolean canManage = canManageTicket(ticket, currentUser);
        boolean assigneeOnly = isAssigneeOnly(ticket, currentUser);
        boolean extensionPendiente = Boolean.TRUE.equals(ticket.getExtensionPendiente());

        TicketPendingExtensionDto pendingExtension = null;
        if (extensionPendiente) {
            String solicitadoPor = null;
            if (ticket.getExtensionSolicitadaPorUserId() != null) {
                solicitadoPor = userRepository.findById(ticket.getExtensionSolicitadaPorUserId())
                        .map(User::getNombre)
                        .orElse(null);
            }
            pendingExtension = TicketPendingExtensionDto.builder()
                    .fechaSolicitada(ticket.getExtensionFechaSolicitada())
                    .motivo(ticket.getExtensionMotivo())
                    .solicitadoPor(solicitadoPor)
                    .build();
        }

        return TicketDto.builder()
                .id(ticket.getId())
                .teamId(ticket.getTeamId())
                .teamNombre(teamNombre)
                .codigo(ticket.getCodigo())
                .nombre(ticket.getNombre())
                .descripcion(ticket.getDescripcion())
                .asignadoPor(ticket.getAsignadoPor())
                .fechaInicio(ticket.getFechaInicio())
                .fechaFin(ticket.getFechaFin())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .assignedMembers(assignedMembers)
                .canEdit(canManage)
                .canDelete(canManage)
                .canExtendDirectly(canManage)
                .canRequestExtension(assigneeOnly && !extensionPendiente)
                .canReviewExtension(canManage && extensionPendiente)
                .pendingExtension(pendingExtension)
                .build();
    }
}
