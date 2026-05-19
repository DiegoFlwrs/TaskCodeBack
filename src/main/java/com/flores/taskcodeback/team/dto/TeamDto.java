package com.flores.taskcodeback.team.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamDto {
    private UUID id;
    private String nombre;
    private String descripcion;
    private String codigo;
    private List<TeamMemberDto> members;
    private LocalDateTime createdAt;
}

