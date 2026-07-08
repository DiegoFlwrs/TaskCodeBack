package com.flores.taskcodeback.ticket.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAssignedMemberDto {
    private UUID id;
    private String nombre;
    private String email;
}
