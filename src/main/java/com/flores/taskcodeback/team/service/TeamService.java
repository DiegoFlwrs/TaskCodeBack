package com.flores.taskcodeback.team.service;

import com.flores.taskcodeback.team.dto.*;

import java.util.List;
import java.util.UUID;

public interface TeamService {
    List<TeamDto> getTeams(String email);
    TeamDto createTeam(String email, TeamRequestDto request);
    TeamDto updateTeam(String email, UUID id, TeamRequestDto request);
    void deleteTeam(String email, UUID id);

    TeamMemberDto addMember(String email, UUID teamId, TeamMemberRequestDto request);
    TeamMemberDto updateMember(String email, UUID teamId, UUID memberId, TeamMemberRequestDto request);
    DeleteMemberResultDto deleteMember(String email, UUID teamId, UUID memberId);
}

