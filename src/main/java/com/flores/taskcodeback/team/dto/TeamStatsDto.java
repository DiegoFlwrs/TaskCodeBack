package com.flores.taskcodeback.team.dto;

import com.flores.taskcodeback.team.entity.TeamMember;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamStatsDto {

    private UUID memberId;
    private String memberNombre;
    private TeamMember.MemberRole memberRole;

    private TaskStats tasks;
    private TicketStats tickets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStats {
        private long total;
        private long pendiente;
        private long enProgreso;
        private long completada;
        private long cancelada;
        private long consultar;
        private long tiempoTotalMinutos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketStats {
        private long total;
        private long activo;
        private long completado;
        private long cancelado;
        private PorPrioridad porPrioridad;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PorPrioridad {
        private long alta;
        private long media;
        private long baja;
    }
}

