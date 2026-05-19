package com.flores.taskcodeback.app.repository;

import com.flores.taskcodeback.app.entity.Aplicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppRepository extends JpaRepository<Aplicacion, UUID> {
    List<Aplicacion> findByUserIdOrderByNombreAsc(Long userId);
}

