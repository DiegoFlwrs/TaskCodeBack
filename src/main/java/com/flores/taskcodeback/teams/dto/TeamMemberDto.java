package com.flores.taskcodeback.teams.dto;

import com.flores.taskcodeback.teams.entity.TeamMember;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberDto {
    private UUID id;
    private Long userId;
    private String nombre;
    private String email;
    private TeamMember.MemberRole role;
    private TeamMember.MemberStatus status;
}

