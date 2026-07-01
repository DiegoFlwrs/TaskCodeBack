package com.flores.taskcodeback.ticket.controller;

import com.flores.taskcodeback.ticket.dto.TicketDto;
import com.flores.taskcodeback.ticket.dto.TicketExtensionRequestDto;
import com.flores.taskcodeback.ticket.dto.TicketExtensionReviewDto;
import com.flores.taskcodeback.ticket.dto.TicketRequestDto;
import com.flores.taskcodeback.ticket.entity.Ticket;
import com.flores.taskcodeback.ticket.service.TicketService;
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
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<List<TicketDto>> getTickets(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Ticket.TicketStatus status) {
        return ResponseEntity.ok(ticketService.getTickets(userDetails.getUsername(), status));
    }

    @PostMapping
    public ResponseEntity<TicketDto> createTicket(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TicketRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(userDetails.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketDto> updateTicket(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @RequestBody TicketRequestDto request) {
        return ResponseEntity.ok(ticketService.updateTicket(userDetails.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        ticketService.deleteTicket(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/extension-request")
    public ResponseEntity<TicketDto> requestExtension(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody TicketExtensionRequestDto request) {
        return ResponseEntity.ok(ticketService.requestExtension(userDetails.getUsername(), id, request));
    }

    @PostMapping("/{id}/extension-review")
    public ResponseEntity<TicketDto> reviewExtension(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody TicketExtensionReviewDto review) {
        return ResponseEntity.ok(ticketService.reviewExtension(userDetails.getUsername(), id, review));
    }
}

