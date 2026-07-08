package com.flores.taskcodeback.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppRequestDto {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
    private String descripcion;

    @Size(max = 500, message = "La URL no puede superar 500 caracteres")
    private String url;

    @Size(max = 30, message = "El color no puede superar 30 caracteres")
    private String color;
}

