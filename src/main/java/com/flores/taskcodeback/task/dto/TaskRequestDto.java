package com.flores.taskcodeback.task.dto;

import com.flores.taskcodeback.task.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDto {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;

    @Size(max = 50, message = "RQ/Ticket no puede superar 50 caracteres")
    private String rqTicket;

    @Size(max = 100, message = "Solicitante no puede superar 100 caracteres")
    private String solicitante;

    @Size(max = 150, message = "Aplicación no puede superar 150 caracteres")
    private String aplicacion;

    @Size(max = 5000, message = "Observación no puede superar 5000 caracteres")
    private String observacion;

    @Size(max = 5000, message = "Consulta/observación no puede superar 5000 caracteres")
    private String consultaObservacion;

    @Size(max = 500, message = "URL no puede superar 500 caracteres")
    private String urlEscenario;
    private Task.TaskStatus status;
    private Task.TaskPriority priority;
    private String horaInicio;
    private String horaFin;
    private String tiempoInvertido;

    @NotNull(message = "La fecha es requerida")
    private LocalDate fecha;
}

