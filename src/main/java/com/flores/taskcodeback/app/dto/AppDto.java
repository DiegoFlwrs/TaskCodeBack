package com.flores.taskcodeback.app.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppDto {
    private UUID id;
    private String nombre;
    private String descripcion;
    private String url;
    private String color;
    private LocalDateTime createdAt;
}

