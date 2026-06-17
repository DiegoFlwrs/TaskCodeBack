package com.flores.taskcodeback.task.dto;

import com.flores.taskcodeback.task.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDto {

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    private String rqTicket;
    private String solicitante;
    private String aplicacion;
    private String observacion;
    private String consultaObservacion;
    private String urlEscenario;
    private Task.TaskStatus status;
    private Task.TaskPriority priority;
    private String horaInicio;
    private String horaFin;
    private String tiempoInvertido;

    @NotNull(message = "La fecha es requerida")
    private LocalDate fecha;
}

