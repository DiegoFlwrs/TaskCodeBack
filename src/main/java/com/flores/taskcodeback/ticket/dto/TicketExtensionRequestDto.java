package com.flores.taskcodeback.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketExtensionRequestDto {

    @NotNull(message = "La nueva fecha es requerida")
    private LocalDate fechaFin;

    @NotBlank(message = "El motivo es requerido")
    private String motivo;
}
