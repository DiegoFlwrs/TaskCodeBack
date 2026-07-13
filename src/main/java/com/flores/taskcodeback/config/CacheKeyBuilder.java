package com.flores.taskcodeback.config;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component("cacheKeyBuilder")
public class CacheKeyBuilder {

    public String taskKey(LocalDate fecha, LocalDate fechaInicio, LocalDate fechaFin,
                          Integer page, Integer size, String rqTicket, String aplicacion, String search) {
        StringBuilder key = new StringBuilder("p:");
        key.append(page != null ? page : 0).append(':').append(size != null ? size : 5).append(':');
        if (fecha != null) {
            key.append("date:").append(fecha);
        } else if (fechaInicio != null && fechaFin != null) {
            key.append("range:").append(fechaInicio).append(':').append(fechaFin);
        } else {
            key.append("all");
        }
        key.append(':').append(rqTicket != null ? rqTicket : "");
        key.append(':').append(aplicacion != null ? aplicacion : "");
        key.append(':').append(search != null ? search : "");
        return key.toString();
    }

    public String ticketKey(String status, Integer page, Integer size, String search) {
        return (status != null ? status : "all") + ":p:" + (page != null ? page : 0)
                + ":" + (size != null ? size : 5) + ":" + (search != null ? search : "");
    }

    public String appKey(Integer page, Integer size, String search) {
        return "p:" + (page != null ? page : 0) + ":" + (size != null ? size : 5)
                + ":" + (search != null ? search : "");
    }

    public String teamStatsKey(UUID teamId, LocalDate fechaInicio, LocalDate fechaFin, UUID memberId) {
        return teamId + ":" + fechaInicio + ":" + fechaFin + ":" + (memberId != null ? memberId : "all");
    }
}
