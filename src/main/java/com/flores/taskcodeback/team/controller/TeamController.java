package com.flores.taskcodeback.team.controller;

import com.flores.taskcodeback.team.dto.*;
import com.flores.taskcodeback.team.service.TeamService;
import com.flores.taskcodeback.team.service.TeamStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamStatsService teamStatsService;

    @GetMapping
    public ResponseEntity<List<TeamDto>> getTeams(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(teamService.getTeams(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<TeamDto> createTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TeamRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(userDetails.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamDto> updateTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody TeamRequestDto request) {
        return ResponseEntity.ok(teamService.updateTeam(userDetails.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        teamService.deleteTeam(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<TeamMemberDto> addMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody TeamMemberRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.addMember(userDetails.getUsername(), id, request));
    }

    @PutMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<TeamMemberDto> updateMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID teamId,
            @PathVariable UUID memberId,
            @Valid @RequestBody TeamMemberRequestDto request) {
        return ResponseEntity.ok(teamService.updateMember(userDetails.getUsername(), teamId, memberId, request));
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<DeleteMemberResultDto> deleteMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID teamId,
            @PathVariable UUID memberId) {
        return ResponseEntity.ok(teamService.deleteMember(userDetails.getUsername(), teamId, memberId));
    }

    @GetMapping("/{teamId}/stats")
    public ResponseEntity<List<TeamStatsDto>> getStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) UUID memberId) {
        return ResponseEntity.ok(teamStatsService.getStats(
                userDetails.getUsername(), teamId, fechaInicio, fechaFin, memberId));
    }
}
