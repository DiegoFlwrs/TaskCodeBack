package com.flores.taskcodeback.team.service;

import com.flores.taskcodeback.team.dto.TeamStatsDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TeamStatsService {

    List<TeamStatsDto> getStats(
            String email,
            UUID teamId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            UUID memberId);
}
