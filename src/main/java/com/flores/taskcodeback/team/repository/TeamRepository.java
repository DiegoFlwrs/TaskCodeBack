package com.flores.taskcodeback.team.repository;

import com.flores.taskcodeback.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    boolean existsByCodigo(String codigo);
}

