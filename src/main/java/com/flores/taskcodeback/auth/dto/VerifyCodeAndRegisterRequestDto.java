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
public class VerifyCodeAndRegisterRequestDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100)
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 50)
    private String password;

    @NotBlank(message = "El código de verificación es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "El código debe tener 6 caracteres alfanuméricos")
    private String verificationCode;
}
