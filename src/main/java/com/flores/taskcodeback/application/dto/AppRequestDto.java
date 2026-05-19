package com.flores.taskcodeback.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppRequestDto {

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    private String descripcion;
    private String url;
    private String color;
}

