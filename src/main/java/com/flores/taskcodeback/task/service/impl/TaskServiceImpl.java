package com.flores.taskcodeback.task.service.impl;

import com.flores.taskcodeback.exception.BadRequestException;
import com.flores.taskcodeback.exception.ResourceNotFoundException;
import com.flores.taskcodeback.task.dto.TaskDto;
import com.flores.taskcodeback.task.dto.TaskRequestDto;
import com.flores.taskcodeback.task.entity.Task;
import com.flores.taskcodeback.task.repository.TaskRepository;
import com.flores.taskcodeback.task.service.TaskService;
import com.flores.taskcodeback.ticket.repository.TicketRepository;
import com.flores.taskcodeback.user.entity.User;
import com.flores.taskcodeback.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getTasks(String email, LocalDate fecha, LocalDate fechaInicio, LocalDate fechaFin) {
        User user = getUser(email);
        List<Task> tasks;

        if (fecha != null) {
            tasks = taskRepository.findByUserIdAndFechaOrderByCreatedAtDesc(user.getId(), fecha);
        } else if (fechaInicio != null && fechaFin != null) {
            if (fechaInicio.isAfter(fechaFin)) {
                throw new BadRequestException("fechaInicio no puede ser posterior a fechaFin");
            }
            tasks = taskRepository.findByUserIdAndFechaBetween(user.getId(), fechaInicio, fechaFin);
        } else {
            tasks = taskRepository.findByUserIdOrderByFechaDescCreatedAtDesc(user.getId());
        }

        return tasks.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public TaskDto createTask(String email, TaskRequestDto request) {
        User user = getUser(email);

        Task task = Task.builder()
                .user(user)
                .nombre(request.getNombre())
                .rqTicket(request.getRqTicket())
                .solicitante(resolveSolicitante(request.getSolicitante(), request.getRqTicket()))
                .aplicacion(request.getAplicacion())
                .observacion(request.getObservacion())
                .consultaObservacion(request.getConsultaObservacion())
                .urlEscenario(request.getUrlEscenario())
                .status(request.getStatus() != null ? request.getStatus() : Task.TaskStatus.PENDIENTE)
                .priority(request.getPriority() != null ? request.getPriority() : Task.TaskPriority.MEDIA)
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .tiempoInvertido(request.getTiempoInvertido())
                .fecha(request.getFecha())
                .teamId(inferTeamId(request.getRqTicket()))
                .build();

        return toDto(taskRepository.save(task));
    }

    @Override
    public TaskDto updateTask(String email, UUID id, TaskRequestDto request) {
        User user = getUser(email);
        Task task = getTaskForUser(id, user.getId());

        if (request.getNombre() != null) task.setNombre(request.getNombre());
        if (request.getAplicacion() != null) task.setAplicacion(request.getAplicacion());
        if (request.getObservacion() != null) task.setObservacion(request.getObservacion());
        if (request.getConsultaObservacion() != null) task.setConsultaObservacion(request.getConsultaObservacion());
        if (request.getUrlEscenario() != null) task.setUrlEscenario(request.getUrlEscenario());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getHoraInicio() != null) task.setHoraInicio(request.getHoraInicio());
        if (request.getHoraFin() != null) task.setHoraFin(request.getHoraFin());
        if (request.getTiempoInvertido() != null) task.setTiempoInvertido(request.getTiempoInvertido());
        if (request.getFecha() != null) task.setFecha(request.getFecha());

        // Re-evaluar teamId y solicitante si cambia el rqTicket
        if (request.getRqTicket() != null) {
            task.setRqTicket(request.getRqTicket());
            task.setTeamId(inferTeamId(request.getRqTicket()));
            if (request.getSolicitante() == null || request.getSolicitante().isBlank()) {
                task.setSolicitante(inferSolicitante(request.getRqTicket()));
            }
        }
        if (request.getSolicitante() != null) task.setSolicitante(request.getSolicitante());

        return toDto(taskRepository.save(task));
    }

    @Override
    public void deleteTask(String email, UUID id) {
        User user = getUser(email);
        Task task = getTaskForUser(id, user.getId());
        taskRepository.delete(task);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Si el código de ticket existe en la BD, retorna su teamId.
     * Si no existe o el código es vacío → null (tarea personal/sin equipo).
     */
    private UUID inferTeamId(String rqTicket) {
        if (rqTicket == null || rqTicket.isBlank()) return null;
        return ticketRepository.findFirstByCodigoOrderByCreatedAtDesc(rqTicket)
                .map(ticket -> ticket.getTeamId())
                .orElse(null);
    }

    private String inferSolicitante(String rqTicket) {
        if (rqTicket == null || rqTicket.isBlank()) return null;
        return ticketRepository.findFirstByCodigoOrderByCreatedAtDesc(rqTicket)
                .map(ticket -> ticket.getAsignadoPor())
                .orElse(null);
    }

    private String resolveSolicitante(String solicitante, String rqTicket) {
        if (solicitante != null && !solicitante.isBlank()) return solicitante;
        return inferSolicitante(rqTicket);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Task getTaskForUser(UUID id, Long userId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));
        if (!task.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes permisos para acceder a esta tarea");
        }
        return task;
    }

    private TaskDto toDto(Task task) {
        return TaskDto.builder()
                .id(task.getId())
                .nombre(task.getNombre())
                .rqTicket(task.getRqTicket())
                .solicitante(task.getSolicitante())
                .aplicacion(task.getAplicacion())
                .observacion(task.getObservacion())
                .consultaObservacion(task.getConsultaObservacion())
                .urlEscenario(task.getUrlEscenario())
                .status(task.getStatus())
                .priority(task.getPriority())
                .horaInicio(task.getHoraInicio())
                .horaFin(task.getHoraFin())
                .tiempoInvertido(task.getTiempoInvertido())
                .fecha(task.getFecha())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
