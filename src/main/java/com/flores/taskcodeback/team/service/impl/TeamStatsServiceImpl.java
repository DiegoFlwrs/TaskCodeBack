package com.flores.taskcodeback.team.service.impl;

import com.flores.taskcodeback.config.CacheNames;
import com.flores.taskcodeback.exception.BadRequestException;
import com.flores.taskcodeback.exception.ResourceNotFoundException;
import com.flores.taskcodeback.task.entity.Task;
import com.flores.taskcodeback.task.repository.TaskRepository;
import com.flores.taskcodeback.team.dto.TeamStatsDto;
import com.flores.taskcodeback.team.entity.Team;
import com.flores.taskcodeback.team.entity.TeamMember;
import com.flores.taskcodeback.team.repository.TeamMemberRepository;
import com.flores.taskcodeback.team.repository.TeamRepository;
import com.flores.taskcodeback.team.service.TeamStatsService;
import com.flores.taskcodeback.ticket.entity.Ticket;
import com.flores.taskcodeback.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamStatsServiceImpl implements TeamStatsService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskRepository taskRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Cacheable(
            value = CacheNames.TEAM_STATS,
            key = "@cacheKeyBuilder.teamStatsKey(#teamId, #fechaInicio, #fechaFin, #memberId)"
    )
    @Transactional(readOnly = true)
    public List<TeamStatsDto> getStats(
            String email,
            UUID teamId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            UUID memberId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo no encontrado"));

        boolean isOwner = team.getOwner().getEmail().equalsIgnoreCase(email);
        boolean isMember = teamMemberRepository.findByTeamId(teamId).stream()
                .anyMatch(m -> email.equalsIgnoreCase(m.getEmail()));
        if (!isOwner && !isMember) {
            throw new AccessDeniedException("No tienes acceso a las estadísticas de este equipo");
        }

        if (fechaInicio.isAfter(fechaFin)) {
            throw new BadRequestException("fechaInicio no puede ser posterior a fechaFin");
        }

        List<TeamMember> allMembers = teamMemberRepository.findByTeamId(teamId);
        List<TeamMember> members = memberId != null
                ? allMembers.stream().filter(m -> m.getId().equals(memberId)).collect(Collectors.toList())
                : allMembers;

        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = members.stream()
                .filter(m -> m.getUserId() != null)
                .map(TeamMember::getUserId)
                .collect(Collectors.toList());

        List<Task> allTasks = userIds.isEmpty()
                ? Collections.emptyList()
                : taskRepository.findByUserIdInAndTeamIdAndFechaBetween(userIds, teamId, fechaInicio, fechaFin);

        List<Ticket> allTickets = userIds.isEmpty()
                ? Collections.emptyList()
                : ticketRepository.findByUserIdInAndTeamIdAndFechaInicioBetween(userIds, teamId, fechaInicio, fechaFin);

        Map<Long, List<Task>> tasksByUser = allTasks.stream()
                .collect(Collectors.groupingBy(t -> t.getUser().getId()));
        Map<Long, List<Ticket>> ticketsByUser = allTickets.stream()
                .collect(Collectors.groupingBy(t -> t.getUser().getId()));

        return members.stream().map(member -> {
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
    }

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

    private long parseTiempoToMinutos(String tiempo) {
        if (tiempo == null || tiempo.isBlank()) return 0L;
        String t = tiempo.trim().toLowerCase();

        if (t.contains("h") || t.contains("m")) {
            long horas = 0, minutos = 0;
            java.util.regex.Matcher mh = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)h").matcher(t);
            java.util.regex.Matcher mm = java.util.regex.Pattern.compile("(\\d+)m").matcher(t);
            if (mh.find()) horas = (long) (Double.parseDouble(mh.group(1)) * 60);
            if (mm.find()) minutos = Long.parseLong(mm.group(1));
            return horas + minutos;
        }

        if (t.contains(":")) {
            String[] parts = t.split(":");
            try {
                return Long.parseLong(parts[0].trim()) * 60 + Long.parseLong(parts[1].trim());
            } catch (Exception e) {
                return 0L;
            }
        }

        try {
            return Long.parseLong(t);
        } catch (Exception e) {
            return 0L;
        }
    }
}
