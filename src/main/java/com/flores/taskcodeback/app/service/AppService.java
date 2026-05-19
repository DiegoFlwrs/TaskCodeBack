package com.flores.taskcodeback.app.service;

import com.flores.taskcodeback.app.dto.AppDto;
import com.flores.taskcodeback.app.dto.AppRequestDto;

import java.util.List;
import java.util.UUID;

public interface AppService {
    List<AppDto> getApps(String email);
    AppDto createApp(String email, AppRequestDto request);
    AppDto updateApp(String email, UUID id, AppRequestDto request);
    void deleteApp(String email, UUID id);
}

