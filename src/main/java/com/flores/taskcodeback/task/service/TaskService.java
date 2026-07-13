package com.flores.taskcodeback.task.service;

import com.flores.taskcodeback.common.dto.PageResponse;
import com.flores.taskcodeback.task.dto.TaskDateSummaryDto;
import com.flores.taskcodeback.task.dto.TaskDto;
import com.flores.taskcodeback.task.dto.TaskRequestDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaskService {
    PageResponse<TaskDto> getTasks(String email, LocalDate fecha, LocalDate fechaInicio, LocalDate fechaFin,
                                   String rqTicket, String aplicacion, String search, Integer page, Integer size);
    List<TaskDateSummaryDto> getTaskDateSummaries(String email);
    TaskDto createTask(String email, TaskRequestDto request);
    TaskDto updateTask(String email, UUID id, TaskRequestDto request);
    void deleteTask(String email, UUID id);
}

