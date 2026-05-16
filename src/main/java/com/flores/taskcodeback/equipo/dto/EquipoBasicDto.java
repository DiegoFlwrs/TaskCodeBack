package com.flores.taskcodeback.equipo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipoBasicDto {

    private Long id;
    private String nombre;
    private String descripcion;
    private String codigo;
}
