package com.flores.taskcodeback.application.service.impl;

import com.flores.taskcodeback.application.dto.AppDto;
import com.flores.taskcodeback.application.dto.AppRequestDto;
import com.flores.taskcodeback.application.entity.Aplicacion;
import com.flores.taskcodeback.application.repository.AppRepository;
import com.flores.taskcodeback.application.service.AppService;
import com.flores.taskcodeback.exception.ResourceNotFoundException;
import com.flores.taskcodeback.user.entity.User;
import com.flores.taskcodeback.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppServiceImpl implements AppService {

    private final AppRepository appRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AppDto> getApps(String email) {
        User user = getUser(email);
        return appRepository.findByUserIdOrderByNombreAsc(user.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public AppDto createApp(String email, AppRequestDto request) {
        User user = getUser(email);
        Aplicacion app = Aplicacion.builder()
                .user(user)
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .url(request.getUrl())
                .color(request.getColor())
                .build();
        return toDto(appRepository.save(app));
    }

    @Override
    public AppDto updateApp(String email, UUID id, AppRequestDto request) {
        User user = getUser(email);
        Aplicacion app = getAppForUser(id, user.getId());

        if (request.getNombre() != null) app.setNombre(request.getNombre());
        if (request.getDescripcion() != null) app.setDescripcion(request.getDescripcion());
        if (request.getUrl() != null) app.setUrl(request.getUrl());
        if (request.getColor() != null) app.setColor(request.getColor());

        return toDto(appRepository.save(app));
    }

    @Override
    public void deleteApp(String email, UUID id) {
        User user = getUser(email);
        Aplicacion app = getAppForUser(id, user.getId());
        appRepository.delete(app);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Aplicacion getAppForUser(UUID id, Long userId) {
        Aplicacion app = appRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aplicación no encontrada"));
        if (!app.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("No tienes permisos para acceder a esta aplicación");
        }
        return app;
    }

    private AppDto toDto(Aplicacion app) {
        return AppDto.builder()
                .id(app.getId())
                .nombre(app.getNombre())
                .descripcion(app.getDescripcion())
                .url(app.getUrl())
                .color(app.getColor())
                .createdAt(app.getCreatedAt())
                .build();
    }
}

