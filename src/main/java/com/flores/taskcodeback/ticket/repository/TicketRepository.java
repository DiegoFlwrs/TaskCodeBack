package com.flores.taskcodeback.ticket.repository;

import com.flores.taskcodeback.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Ticket> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Ticket.TicketStatus status);

    /** Tickets del equipo para un conjunto de usuarios, filtrado por fechaInicio del ticket */
    @Query("SELECT t FROM Ticket t WHERE t.user.id IN :userIds AND t.teamId = :teamId AND t.fechaInicio BETWEEN :start AND :end")
    List<Ticket> findByUserIdInAndTeamIdAndFechaInicioBetween(@Param("userIds") List<Long> userIds,
                                                               @Param("teamId") UUID teamId,
                                                               @Param("start") LocalDate start,
                                                               @Param("end") LocalDate end);

    /** Buscar ticket por su código para inferir el equipo al crear una tarea */
    Optional<Ticket> findFirstByCodigoOrderByCreatedAtDesc(String codigo);
}
