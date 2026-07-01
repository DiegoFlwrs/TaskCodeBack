package com.flores.taskcodeback.ticket.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketPendingExtensionDto {
    private LocalDate fechaSolicitada;
    private String motivo;
    private String solicitadoPor;
}
