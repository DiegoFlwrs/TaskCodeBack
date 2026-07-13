package com.flores.taskcodeback.task.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDateSummaryDto {
    private LocalDate fecha;
    private long count;
    private long completedCount;

    public boolean isAllCompleted() {
        return count > 0 && count == completedCount;
    }
}
