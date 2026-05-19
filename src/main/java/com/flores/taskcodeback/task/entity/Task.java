package com.flores.taskcodeback.task.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.flores.taskcodeback.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(name = "rq_ticket", length = 100)
    private String rqTicket;

    @Column(length = 200)
    private String aplicacion;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "url_escenario", length = 500)
    private String urlEscenario;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIA;

    @Column(name = "hora_inicio", length = 10)
    private String horaInicio;

    @Column(name = "hora_fin", length = 10)
    private String horaFin;

    @Column(name = "tiempo_invertido", length = 50)
    private String tiempoInvertido;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TaskStatus {
        PENDIENTE("pendiente"),
        EN_PROGRESO("en-progreso"),
        COMPLETADA("completada"),
        CANCELADA("cancelada");

        private final String value;

        TaskStatus(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static TaskStatus fromValue(String value) {
            for (TaskStatus s : values()) {
                if (s.value.equalsIgnoreCase(value)) return s;
            }
            throw new IllegalArgumentException("Status inválido: " + value);
        }
    }

    public enum TaskPriority {
        BAJA("baja"),
        MEDIA("media"),
        ALTA("alta"),
        CRITICA("critica");

        private final String value;

        TaskPriority(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static TaskPriority fromValue(String value) {
            for (TaskPriority p : values()) {
                if (p.value.equalsIgnoreCase(value)) return p;
            }
            throw new IllegalArgumentException("Priority inválida: " + value);
        }
    }
}

