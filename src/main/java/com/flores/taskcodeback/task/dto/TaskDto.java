package com.flores.taskcodeback.task.dto;

import com.flores.taskcodeback.task.entity.Task;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {
    private UUID id;
    private String nombre;
    private String rqTicket;
    private String aplicacion;
    private String observacion;
    private String consultaObservacion;
    private String urlEscenario;
    private Task.TaskStatus status;
    private Task.TaskPriority priority;
    private String horaInicio;
    private String horaFin;
    private String tiempoInvertido;
    private LocalDate fecha;
    private LocalDateTime createdAt;
}

