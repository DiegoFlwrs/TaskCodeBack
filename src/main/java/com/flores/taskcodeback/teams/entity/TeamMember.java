package com.flores.taskcodeback.teams.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_team_members")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private MemberRole role = MemberRole.DEVELOPER;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum MemberRole {
        LEADER("LEADER"),
        DEVELOPER("DEVELOPER"),
        QA("QA"),
        DESIGNER("DESIGNER"),
        DEVOPS("DEVOPS");

        private final String value;

        MemberRole(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static MemberRole fromValue(String value) {
            for (MemberRole r : values()) {
                if (r.value.equalsIgnoreCase(value)) return r;
            }
            throw new IllegalArgumentException("Role inválido: " + value);
        }
    }

    public enum MemberStatus {
        ACTIVO("activo"),
        INACTIVO("inactivo");

        private final String value;

        MemberStatus(String value) { this.value = value; }

        @JsonValue
        public String getValue() { return value; }

        @JsonCreator
        public static MemberStatus fromValue(String value) {
            for (MemberStatus s : values()) {
                if (s.value.equalsIgnoreCase(value)) return s;
            }
            throw new IllegalArgumentException("Status inválido: " + value);
        }
    }
}

