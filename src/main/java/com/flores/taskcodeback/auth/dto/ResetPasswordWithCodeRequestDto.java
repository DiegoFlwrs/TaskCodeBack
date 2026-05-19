package com.flores.taskcodeback.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordWithCodeRequestDto {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email invalido")
    private String email;

    @NotBlank(message = "El codigo de verificacion es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "El codigo debe tener 6 caracteres alfanumericos")
    private String verificationCode;

    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Size(min = 6, max = 50, message = "La nueva contrasena debe tener entre 6 y 50 caracteres")
    private String newPassword;

    @NotBlank(message = "La confirmacion de contrasena es obligatoria")
    @Size(min = 6, max = 50, message = "La confirmacion de contrasena debe tener entre 6 y 50 caracteres")
    private String confirmPassword;
}

