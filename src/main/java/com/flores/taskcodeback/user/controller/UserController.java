package com.flores.taskcodeback.user.controller;

import com.flores.taskcodeback.exception.ResourceNotFoundException;
import com.flores.taskcodeback.user.dto.UserDto;
import com.flores.taskcodeback.user.entity.User;
import com.flores.taskcodeback.user.repository.UserRepository;
import com.flores.taskcodeback.user.service.UserQueryService;
import com.flores.taskcodeback.workspace.dto.EquipoBasicDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserQueryService userQueryService;

    @GetMapping
    public ResponseEntity<List<UserDto>> getTeamMembers(@AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(userQueryService.getTeamMembers(principal.getEmail()));
    }

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
