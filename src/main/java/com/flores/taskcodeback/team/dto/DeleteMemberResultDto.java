package com.flores.taskcodeback.team.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteMemberResultDto {
    private boolean accountDeleted;
    private String message;
}
