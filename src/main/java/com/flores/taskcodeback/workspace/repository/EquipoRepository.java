package com.flores.taskcodeback.workspace.repository;

import com.flores.taskcodeback.workspace.entity.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    Optional<Equipo> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Equipo> findByActivoTrue();

    Optional<Equipo> findByLeaderIdAndActivoTrue(Long leaderId);
}
