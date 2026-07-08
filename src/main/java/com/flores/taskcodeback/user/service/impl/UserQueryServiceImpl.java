package com.flores.taskcodeback.user.service.impl;

import com.flores.taskcodeback.config.CacheNames;
import com.flores.taskcodeback.exception.ResourceNotFoundException;
import com.flores.taskcodeback.team.entity.TeamMember;
import com.flores.taskcodeback.team.repository.TeamMemberRepository;
import com.flores.taskcodeback.team.repository.TeamRepository;
import com.flores.taskcodeback.user.dto.UserDto;
import com.flores.taskcodeback.user.entity.User;
import com.flores.taskcodeback.user.repository.UserRepository;
import com.flores.taskcodeback.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Override
    @Cacheable(value = CacheNames.TEAM_MEMBERS, key = "#email")
    @Transactional(readOnly = true)
    public List<UserDto> getTeamMembers(String email) {
        User me = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Set<UUID> teamIds = new LinkedHashSet<>();
        teamRepository.findByOwnerIdOrderByCreatedAtDesc(me.getId())
                .forEach(t -> teamIds.add(t.getId()));
        teamMemberRepository.findByUserId(me.getId())
                .forEach(tm -> teamIds.add(tm.getTeam().getId()));

        if (teamIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, UserDto> membersMap = new LinkedHashMap<>();
        for (UUID teamId : teamIds) {
            List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
            for (TeamMember tm : members) {
                if (tm.getUserId() == null || tm.getUserId().equals(me.getId())) continue;
                if (membersMap.containsKey(tm.getUserId())) continue;

                userRepository.findById(tm.getUserId()).ifPresent(u ->
                        membersMap.put(u.getId(), UserDto.builder()
                                .id(u.getId())
                                .nombre(u.getNombre())
                                .email(u.getEmail())
                                .role(u.getRole())
                                .activo(u.getActivo())
                                .build())
                );
            }
        }

        return new ArrayList<>(membersMap.values());
    }
}
