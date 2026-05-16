package com.flores.taskcodeback.auth.dto;

import com.flores.taskcodeback.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    private String token;
    private String tokenType;
    private Long expiresIn;
    private UserDto user;

    @Builder.Default
    private String tokenTypeString = "Bearer";
}
