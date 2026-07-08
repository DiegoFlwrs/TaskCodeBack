package com.flores.taskcodeback.config;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component("cacheKeyBuilder")
public class CacheKeyBuilder {

    public String taskKey(LocalDate fecha, LocalDate fechaInicio, LocalDate fechaFin) {
        if (fecha != null) {
            return "date:" + fecha;
        }
        if (fechaInicio != null && fechaFin != null) {
            return "range:" + fechaInicio + ":" + fechaFin;
        }
        return "all";
    }

    public String ticketKey(String status) {
        return status != null ? status : "all";
    }

    public String teamStatsKey(UUID teamId, LocalDate fechaInicio, LocalDate fechaFin, UUID memberId) {
        return teamId + ":" + fechaInicio + ":" + fechaFin + ":" + (memberId != null ? memberId : "all");
    }
}
