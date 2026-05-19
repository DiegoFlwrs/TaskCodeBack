package com.flores.taskcodeback.teams.dto;

import com.flores.taskcodeback.teams.entity.TeamMember;
import jakarta.validation.constraints.Email;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberRequestDto {

    /**
     * Si viene informado, se asocia un usuario ya existente (no se crea cuenta nueva).
     * nombre, email y passwordMode se ignoran en ese caso.
     */
    private Long existingUserId;

    /** Requerido solo cuando existingUserId es null (usuario nuevo) */
    private String nombre;

    @Email(message = "El email no es válido")
    private String email;

    private TeamMember.MemberRole role;
    private TeamMember.MemberStatus status;

    /**
     * "auto"   → backend genera contraseña aleatoria segura y la envía por email
     * "manual" → backend usa el campo password recibido (se hashea en servidor)
     */
    private String passwordMode;

    /** Solo obligatorio cuando passwordMode = "manual" */
    private String password;
}
