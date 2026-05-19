package com.flores.taskcodeback.teams.controller;

import com.flores.taskcodeback.teams.dto.*;
import com.flores.taskcodeback.teams.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

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
            @RequestBody TeamRequestDto request) {
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
            @RequestBody TeamMemberRequestDto request) {
        return ResponseEntity.ok(teamService.updateMember(userDetails.getUsername(), teamId, memberId, request));
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<Void> deleteMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID teamId,
            @PathVariable UUID memberId) {
        teamService.deleteMember(userDetails.getUsername(), teamId, memberId);
        return ResponseEntity.noContent().build();
    }
}

