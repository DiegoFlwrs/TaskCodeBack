package com.flores.taskcodeback.task.service;

import com.flores.taskcodeback.task.dto.TaskDto;
import com.flores.taskcodeback.task.dto.TaskRequestDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaskService {
    List<TaskDto> getTasks(String email, LocalDate fecha, LocalDate fechaInicio, LocalDate fechaFin);
    TaskDto createTask(String email, TaskRequestDto request);
    TaskDto updateTask(String email, UUID id, TaskRequestDto request);
    void deleteTask(String email, UUID id);
}

