package com.flores.taskcodeback.team.controller;

import com.flores.taskcodeback.exception.BadRequestException;
import com.flores.taskcodeback.exception.ResourceNotFoundException;
import com.flores.taskcodeback.task.entity.Task;
import com.flores.taskcodeback.task.repository.TaskRepository;
import com.flores.taskcodeback.team.dto.*;
import com.flores.taskcodeback.team.entity.Team;
import com.flores.taskcodeback.team.entity.TeamMember;
import com.flores.taskcodeback.team.repository.TeamMemberRepository;
import com.flores.taskcodeback.team.repository.TeamRepository;
import com.flores.taskcodeback.team.service.TeamService;
import com.flores.taskcodeback.ticket.entity.Ticket;
import com.flores.taskcodeback.ticket.repository.TicketRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskRepository taskRepository;
    private final TicketRepository ticketRepository;

    @GetMapping
    public ResponseEntity<List<TeamDto>> getTeams(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(teamService.getTeams(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<TeamDto> createTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TeamRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(userDetails.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamDto> updateTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody TeamRequestDto request) {
        return ResponseEntity.ok(teamService.updateTeam(userDetails.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        teamService.deleteTeam(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<TeamMemberDto> addMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody TeamMemberRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.addMember(userDetails.getUsername(), id, request));
    }

    @PutMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<TeamMemberDto> updateMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID teamId,
            @PathVariable UUID memberId,
            @Valid @RequestBody TeamMemberRequestDto request) {
        return ResponseEntity.ok(teamService.updateMember(userDetails.getUsername(), teamId, memberId, request));
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<DeleteMemberResultDto> deleteMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID teamId,
            @PathVariable UUID memberId) {
        return ResponseEntity.ok(teamService.deleteMember(userDetails.getUsername(), teamId, memberId));
    }

    /**
     * GET /api/teams/:teamId/stats
     * Query params: fechaInicio (YYYY-MM-DD), fechaFin (YYYY-MM-DD), memberId (UUID, opcional)
     */
    @GetMapping("/{teamId}/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<List<TeamStatsDto>> getStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) UUID memberId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado"));

        String email = userDetails.getUsername();
        boolean isOwner = team.getOwner().getEmail().equalsIgnoreCase(email);
        boolean isMember = teamMemberRepository.findByTeamId(teamId).stream()
                .anyMatch(m -> email.equalsIgnoreCase(m.getEmail()));
        if (!isOwner && !isMember) {
            throw new AccessDeniedException("No tienes acceso a las estadísticas de este equipo");
        }

        if (fechaInicio.isAfter(fechaFin)) {
            throw new BadRequestException("fechaInicio no puede ser posterior a fechaFin");
        }

        // Obtener miembros del equipo
        List<TeamMember> allMembers = teamMemberRepository.findByTeamId(teamId);

        // Filtrar por memberId si se especifica
        List<TeamMember> members = memberId != null
                ? allMembers.stream().filter(m -> m.getId().equals(memberId)).collect(Collectors.toList())
                : allMembers;

        if (members.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // IDs de usuario con cuenta asociada
        List<Long> userIds = members.stream()
                .filter(m -> m.getUserId() != null)
                .map(TeamMember::getUserId)
                .collect(Collectors.toList());

        // Cargar todas las tasks y tickets del período en una sola query
        List<Task> allTasks = userIds.isEmpty()
                ? Collections.emptyList()
                : taskRepository.findByUserIdInAndTeamIdAndFechaBetween(userIds, teamId, fechaInicio, fechaFin);

        List<Ticket> allTickets = userIds.isEmpty()
                ? Collections.emptyList()
                : ticketRepository.findByUserIdInAndTeamIdAndFechaInicioBetween(userIds, teamId, fechaInicio, fechaFin);

        // Agrupar por userId
        Map<Long, List<Task>> tasksByUser = allTasks.stream()
                .collect(Collectors.groupingBy(t -> t.getUser().getId()));
        Map<Long, List<Ticket>> ticketsByUser = allTickets.stream()
                .collect(Collectors.groupingBy(t -> t.getUser().getId()));

        // Construir stats por miembro
        List<TeamStatsDto> result = members.stream().map(member -> {
            List<Task> tasks = member.getUserId() != null
                    ? tasksByUser.getOrDefault(member.getUserId(), Collections.emptyList())
                    : Collections.emptyList();
            List<Ticket> tickets = member.getUserId() != null
                    ? ticketsByUser.getOrDefault(member.getUserId(), Collections.emptyList())
                    : Collections.emptyList();

            return TeamStatsDto.builder()
                    .memberId(member.getId())
                    .memberNombre(member.getNombre())
                    .memberRole(member.getRole())
                    .tasks(buildTaskStats(tasks))
                    .tickets(buildTicketStats(tickets))
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── helpers stats ─────────────────────────────────────────────────────────

    private TeamStatsDto.TaskStats buildTaskStats(List<Task> tasks) {
        long tiempoTotal = tasks.stream()
                .mapToLong(t -> parseTiempoToMinutos(t.getTiempoInvertido()))
                .sum();

        long completada = tasks.stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.COMPLETADA)
                .count();
        long pendiente = tasks.stream()
                .filter(t -> t.getStatus() != Task.TaskStatus.COMPLETADA
                        && t.getStatus() != Task.TaskStatus.CANCELADA)
                .count();
        long tiempoPromedio = completada > 0 ? tiempoTotal / completada : 0;

        return TeamStatsDto.TaskStats.builder()
                .total(tasks.size())
                .pendiente(pendiente)
                .completada(completada)
                .tiempoTotalMinutos(tiempoTotal)
                .tiempoPromedioMinutos(tiempoPromedio)
                .build();
    }

    private TeamStatsDto.TicketStats buildTicketStats(List<Ticket> tickets) {
        LocalDate today = LocalDate.now();
        LocalDate proximoLimite = today.plusDays(3);

        long vencido = tickets.stream()
                .filter(t -> t.getStatus() == Ticket.TicketStatus.ACTIVO
                        && t.getFechaFin().isBefore(today))
                .count();
        long venceProximo = tickets.stream()
                .filter(t -> t.getStatus() == Ticket.TicketStatus.ACTIVO
                        && !t.getFechaFin().isBefore(today)
                        && !t.getFechaFin().isAfter(proximoLimite))
                .count();

        return TeamStatsDto.TicketStats.builder()
                .total(tickets.size())
                .activo(tickets.stream().filter(t -> t.getStatus() == Ticket.TicketStatus.ACTIVO).count())
                .completado(tickets.stream().filter(t -> t.getStatus() == Ticket.TicketStatus.COMPLETADO).count())
                .cancelado(tickets.stream().filter(t -> t.getStatus() == Ticket.TicketStatus.CANCELADO).count())
                .vencido(vencido)
                .venceProximo(venceProximo)
                .porPrioridad(TeamStatsDto.PorPrioridad.builder()
                        .alta(tickets.stream().filter(t -> t.getPriority() == Ticket.TicketPriority.ALTA).count())
                        .media(tickets.stream().filter(t -> t.getPriority() == Ticket.TicketPriority.MEDIA).count())
                        .baja(tickets.stream().filter(t -> t.getPriority() == Ticket.TicketPriority.BAJA).count())
                        .build())
                .build();
    }

    /**
     * Parsea tiempoInvertido a minutos.
     * Formatos soportados: "2h 30m", "1:30", "90m", "2h", "90", "1.5h"
     */
    private long parseTiempoToMinutos(String tiempo) {
        if (tiempo == null || tiempo.isBlank()) return 0L;
        String t = tiempo.trim().toLowerCase();

        // Formato "Xh Ym" o "Xh" o "Ym"
        if (t.contains("h") || t.contains("m")) {
            long horas = 0, minutos = 0;
            java.util.regex.Matcher mh = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)h").matcher(t);
            java.util.regex.Matcher mm = java.util.regex.Pattern.compile("(\\d+)m").matcher(t);
            if (mh.find()) horas = (long)(Double.parseDouble(mh.group(1)) * 60);
            if (mm.find()) minutos = Long.parseLong(mm.group(1));
            return horas + minutos;
        }

        // Formato "H:MM"
        if (t.contains(":")) {
            String[] parts = t.split(":");
            try {
                return Long.parseLong(parts[0].trim()) * 60 + Long.parseLong(parts[1].trim());
            } catch (Exception e) { return 0L; }
        }

        // Solo número → asumir minutos
        try { return Long.parseLong(t); } catch (Exception e) { return 0L; }
    }
}
