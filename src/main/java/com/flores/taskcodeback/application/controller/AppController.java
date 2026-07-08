package com.flores.taskcodeback.application.controller;

import com.flores.taskcodeback.application.dto.AppDto;
import com.flores.taskcodeback.application.dto.AppRequestDto;
import com.flores.taskcodeback.application.service.AppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/apps")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;

    @GetMapping
    public ResponseEntity<List<AppDto>> getApps(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(appService.getApps(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<AppDto> createApp(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AppRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appService.createApp(userDetails.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppDto> updateApp(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody AppRequestDto request) {
        return ResponseEntity.ok(appService.updateApp(userDetails.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApp(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        appService.deleteApp(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}

