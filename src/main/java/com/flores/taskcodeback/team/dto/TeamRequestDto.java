package com.flores.taskcodeback.team.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamRequestDto {

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    private String descripcion;
}

