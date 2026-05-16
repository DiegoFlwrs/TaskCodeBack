package com.flores.taskcodeback.user.dto;


import com.flores.taskcodeback.equipo.dto.EquipoBasicDto;
import com.flores.taskcodeback.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;
    private String nombre;
    private String email;
    private User.Role role;
    private Boolean isIndependent;
    private EquipoBasicDto equipo;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
