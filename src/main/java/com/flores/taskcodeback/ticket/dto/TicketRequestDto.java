package com.flores.taskcodeback.ticket.dto;

import com.flores.taskcodeback.ticket.entity.Ticket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequestDto {

    @NotBlank(message = "El código es requerido")
    private String codigo;

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    private String descripcion;
    private String motivo;
    private String asignadoPor;

    /** ID del equipo al que pertenece este ticket (opcional) */
    private UUID teamId;

    @NotNull(message = "La fecha de inicio es requerida")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es requerida")
    private LocalDate fechaFin;

    private Ticket.TicketPriority priority;
    private Ticket.TicketStatus status;

    /** IDs de miembros del equipo a asignar (solo TEAM_LEADER) */
    private List<UUID> assignedMemberIds;
}

