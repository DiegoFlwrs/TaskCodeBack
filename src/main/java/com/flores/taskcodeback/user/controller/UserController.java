package com.flores.taskcodeback.user.controller;

import com.flores.taskcodeback.workspace.dto.EquipoBasicDto;
import com.flores.taskcodeback.exception.ResourceNotFoundException;
import com.flores.taskcodeback.team.entity.TeamMember;
import com.flores.taskcodeback.team.repository.TeamMemberRepository;
import com.flores.taskcodeback.team.repository.TeamRepository;
import com.flores.taskcodeback.user.dto.UserDto;
import com.flores.taskcodeback.user.entity.User;
import com.flores.taskcodeback.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * GET /api/users
     * Si el usuario es TEAM_LEADER → retorna array con los miembros de sus equipos (excluye al propio líder).
     * Si el usuario es USER miembro → retorna sus compañeros del equipo (también lo excluye a él).
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<UserDto>> getTeamMembers(@AuthenticationPrincipal User principal) {

        User me = userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Reunir los teamId donde este usuario participa (como owner o como miembro)
        Set<UUID> teamIds = new LinkedHashSet<>();

        // Equipos donde es owner (TEAM_LEADER)
        teamRepository.findByOwnerIdOrderByCreatedAtDesc(me.getId())
                .forEach(t -> teamIds.add(t.getId()));

        // Equipos donde es miembro
        teamMemberRepository.findByUserId(me.getId())
                .forEach(tm -> teamIds.add(tm.getTeam().getId()));

        if (teamIds.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Obtener todos los TeamMember de esos equipos, con userId != null, distintos del propio usuario
        Map<Long, UserDto> membersMap = new LinkedHashMap<>();

        for (UUID teamId : teamIds) {
            List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
            for (TeamMember tm : members) {
                if (tm.getUserId() == null) continue;
                if (tm.getUserId().equals(me.getId())) continue; // excluir al propio usuario (líder)

                // Evitar duplicados si el mismo usuario está en varios equipos
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

        return ResponseEntity.ok(new ArrayList<>(membersMap.values()));
    }

    /** GET /api/users/me → perfil del usuario autenticado */
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal User principal) {
        User user = userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        EquipoBasicDto equipoDto = null;
        if (user.getEquipo() != null) {
            equipoDto = EquipoBasicDto.builder()
                    .id(user.getEquipo().getId())
                    .nombre(user.getEquipo().getNombre())
                    .descripcion(user.getEquipo().getDescripcion())
                    .codigo(user.getEquipo().getCodigo())
                    .build();
        }

        return ResponseEntity.ok(UserDto.builder()
                .id(user.getId())
                .nombre(user.getNombre())
                .email(user.getEmail())
                .role(user.getRole())
                .isIndependent(user.getIsIndependent())
                .equipo(equipoDto)
                .activo(user.getActivo())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build());
    }
}
