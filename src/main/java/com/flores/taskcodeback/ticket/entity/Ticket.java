package com.flores.taskcodeback.ticket.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.flores.taskcodeback.team.entity.TeamMember;
import com.flores.taskcodeback.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** ID del equipo (user_teams) al que pertenece este ticket. Nullable para usuarios independientes. */
    @Column(name = "team_id")
    private UUID teamId;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "asignado_por", length = 100)
    private String asignadoPor;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.ACTIVO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ticket_assignments",
            joinColumns = @JoinColumn(name = "ticket_id"),
            inverseJoinColumns = @JoinColumn(name = "team_member_id")
    )
    @Builder.Default
    private List<TeamMember> assignedMembers = new ArrayList<>();

    @Column(name = "extension_pendiente")
    @Builder.Default
    private Boolean extensionPendiente = false;

    @Column(name = "extension_fecha_solicitada")
    private LocalDate extensionFechaSolicitada;

    @Column(name = "extension_motivo", columnDefinition = "TEXT")
    private String extensionMotivo;

    @Column(name = "extension_solicitada_por")
    private Long extensionSolicitadaPorUserId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TicketStatus {
        ACTIVO("activo"),
        COMPLETADO("completado"),
        CANCELADO("cancelado");

        private final String value;

        TicketStatus(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static TicketStatus fromValue(String value) {
            for (TicketStatus s : values()) {
                if (s.value.equalsIgnoreCase(value)) return s;
            }
            throw new IllegalArgumentException("Status inválido: " + value);
        }
    }

    public enum TicketPriority {
        BAJA("baja"),
        MEDIA("media"),
        ALTA("alta");

        private final String value;

        TicketPriority(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static TicketPriority fromValue(String value) {
            for (TicketPriority p : values()) {
                if (p.value.equalsIgnoreCase(value)) return p;
            }
            throw new IllegalArgumentException("Priority inválida: " + value);
        }
    }
}

