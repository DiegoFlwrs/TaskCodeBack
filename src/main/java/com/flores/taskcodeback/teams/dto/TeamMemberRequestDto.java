package com.flores.taskcodeback.teams.dto;

import com.flores.taskcodeback.teams.entity.TeamMember;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberRequestDto {

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email no es válido")
    private String email;

    private TeamMember.MemberRole role;
    private TeamMember.MemberStatus status;

    /**
     * "auto"   → backend genera contraseña aleatoria segura y la envía por email
     * "manual" → backend usa el campo password recibido (se hashea en servidor)
     * Si viene null se trata como "auto"
     */
    private String passwordMode;

    /** Solo obligatorio cuando passwordMode = "manual" */
    private String password;
}

