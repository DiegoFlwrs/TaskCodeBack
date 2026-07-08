package com.flores.taskcodeback.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamRequestDto {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
    private String descripcion;
}

