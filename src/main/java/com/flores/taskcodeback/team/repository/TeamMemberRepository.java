package com.flores.taskcodeback.team.repository;

import com.flores.taskcodeback.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    /** Todos los TeamMember asociados a un userId (para saber en qué equipos está un usuario) */
    List<TeamMember> findByUserId(Long userId);

    /** Todos los miembros de un equipo */
    List<TeamMember> findByTeamId(UUID teamId);
}

