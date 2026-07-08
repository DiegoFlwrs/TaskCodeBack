package com.flores.taskcodeback.ticket.service;

import com.flores.taskcodeback.ticket.dto.TicketDto;
import com.flores.taskcodeback.ticket.dto.TicketExtensionRequestDto;
import com.flores.taskcodeback.ticket.dto.TicketExtensionReviewDto;
import com.flores.taskcodeback.ticket.dto.TicketRequestDto;
import com.flores.taskcodeback.ticket.entity.Ticket;

import java.util.List;
import java.util.UUID;

public interface TicketService {
    List<TicketDto> getTickets(String email, Ticket.TicketStatus status);
    TicketDto createTicket(String email, TicketRequestDto request);
    TicketDto updateTicket(String email, UUID id, TicketRequestDto request);
    void deleteTicket(String email, UUID id);
    TicketDto requestExtension(String email, UUID id, TicketExtensionRequestDto request);
    TicketDto reviewExtension(String email, UUID id, TicketExtensionReviewDto review);
}

