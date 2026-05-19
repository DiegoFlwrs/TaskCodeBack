package com.flores.taskcodeback.ticket.dto;

import com.flores.taskcodeback.ticket.entity.Ticket;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDto {
    private UUID id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String asignadoPor;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Ticket.TicketPriority priority;
    private Ticket.TicketStatus status;
    private LocalDateTime createdAt;
}

