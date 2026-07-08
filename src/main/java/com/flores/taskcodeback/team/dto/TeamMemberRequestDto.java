package com.flores.taskcodeback.team.dto;

import com.flores.taskcodeback.team.entity.TeamMember;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @Email(message = "El email no es válido")
    @Size(max = 150, message = "El email no puede superar 150 caracteres")
    private String email;

    private TeamMember.MemberRole role;
    private TeamMember.MemberStatus status;

    /**
     * "auto"   → backend genera contraseña aleatoria segura y la envía por email
     * "manual" → backend usa el campo password recibido (se hashea en servidor)
     */
    private String passwordMode;

    /** Solo obligatorio cuando passwordMode = "manual" */
    @Size(max = 128, message = "La contraseña no puede superar 128 caracteres")
    private String password;
}
