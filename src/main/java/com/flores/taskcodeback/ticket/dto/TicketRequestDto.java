package com.flores.taskcodeback.ticket.dto;

import com.flores.taskcodeback.ticket.entity.Ticket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 50, message = "El código no puede superar 50 caracteres")
    private String codigo;

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;

    @Size(max = 5000, message = "La descripción no puede superar 5000 caracteres")
    private String descripcion;

    @Size(max = 2000, message = "El motivo no puede superar 2000 caracteres")
    private String motivo;

    @Size(max = 100, message = "Asignado por no puede superar 100 caracteres")
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
    @Size(max = 50, message = "No se pueden asignar más de 50 miembros")
    private List<UUID> assignedMemberIds;
}

