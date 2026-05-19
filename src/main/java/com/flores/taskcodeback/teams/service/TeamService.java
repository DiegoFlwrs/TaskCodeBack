package com.flores.taskcodeback.teams.service;

import com.flores.taskcodeback.teams.dto.*;

import java.util.List;
import java.util.UUID;

public interface TeamService {
    List<TeamDto> getTeams(String email);
    TeamDto createTeam(String email, TeamRequestDto request);
    TeamDto updateTeam(String email, UUID id, TeamRequestDto request);
    void deleteTeam(String email, UUID id);

    TeamMemberDto addMember(String email, UUID teamId, TeamMemberRequestDto request);
    TeamMemberDto updateMember(String email, UUID teamId, UUID memberId, TeamMemberRequestDto request);
    void deleteMember(String email, UUID teamId, UUID memberId);
}

