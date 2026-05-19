package com.flores.taskcodeback.ticket.service.impl;

import com.flores.taskcodeback.exception.ResourceNotFoundException;
import com.flores.taskcodeback.teams.repository.TeamRepository;
import com.flores.taskcodeback.ticket.dto.TicketDto;
import com.flores.taskcodeback.ticket.dto.TicketRequestDto;
import com.flores.taskcodeback.ticket.entity.Ticket;
import com.flores.taskcodeback.ticket.repository.TicketRepository;
import com.flores.taskcodeback.ticket.service.TicketService;
import com.flores.taskcodeback.user.entity.User;
import com.flores.taskcodeback.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TicketDto> getTickets(String email, Ticket.TicketStatus status) {
        User user = getUser(email);
        List<Ticket> tickets = status != null
                ? ticketRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status)
                : ticketRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return tickets.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public TicketDto createTicket(String email, TicketRequestDto request) {
        User user = getUser(email);
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
        return toDto(ticketRepository.save(ticket));
    }

    @Override
    public TicketDto updateTicket(String email, UUID id, TicketRequestDto request) {
        User user = getUser(email);
        Ticket ticket = getTicketForUser(id, user.getId());

        if (request.getCodigo() != null) ticket.setCodigo(request.getCodigo());
        if (request.getNombre() != null) ticket.setNombre(request.getNombre());
        if (request.getDescripcion() != null) ticket.setDescripcion(request.getDescripcion());
        if (request.getAsignadoPor() != null) ticket.setAsignadoPor(request.getAsignadoPor());
        if (request.getTeamId() != null) ticket.setTeamId(request.getTeamId());
        if (request.getFechaInicio() != null) ticket.setFechaInicio(request.getFechaInicio());
        if (request.getFechaFin() != null) ticket.setFechaFin(request.getFechaFin());
        if (request.getPriority() != null) ticket.setPriority(request.getPriority());
        if (request.getStatus() != null) ticket.setStatus(request.getStatus());

        return toDto(ticketRepository.save(ticket));
    }

    @Override
    public void deleteTicket(String email, UUID id) {
        User user = getUser(email);
        Ticket ticket = getTicketForUser(id, user.getId());
        ticketRepository.delete(ticket);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Ticket getTicketForUser(UUID id, Long userId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));
        if (!ticket.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("No tienes permisos para acceder a este ticket");
        }
        return ticket;
    }

    private TicketDto toDto(Ticket ticket) {
        String teamNombre = null;
        if (ticket.getTeamId() != null) {
            teamNombre = teamRepository.findById(ticket.getTeamId())
                    .map(t -> t.getNombre())
                    .orElse(null);
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
                .build();
    }
}
