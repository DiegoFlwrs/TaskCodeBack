package com.flores.taskcodeback.ticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketExtensionReviewDto {

    @NotNull(message = "Debe indicar si aprueba o rechaza")
    private Boolean approved;
}
