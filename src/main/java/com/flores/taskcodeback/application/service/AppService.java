package com.flores.taskcodeback.application.service;

import com.flores.taskcodeback.application.dto.AppDto;
import com.flores.taskcodeback.application.dto.AppRequestDto;

import java.util.List;
import java.util.UUID;

public interface AppService {
    List<AppDto> getApps(String email);
    AppDto createApp(String email, AppRequestDto request);
    AppDto updateApp(String email, UUID id, AppRequestDto request);
    void deleteApp(String email, UUID id);
}

