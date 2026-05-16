package com.flores.taskcodeback.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequestDto {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email invalido")
    private String email;
}

