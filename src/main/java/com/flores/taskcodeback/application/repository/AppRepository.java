package com.flores.taskcodeback.application.repository;

import com.flores.taskcodeback.application.entity.Aplicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppRepository extends JpaRepository<Aplicacion, UUID> {
    List<Aplicacion> findByUserIdOrderByNombreAsc(Long userId);

    @Query("""
            SELECT a FROM Aplicacion a WHERE a.user.id = :userId
            AND (:search IS NULL OR :search = '' OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY a.nombre ASC
            """)
    Page<Aplicacion> findByUserIdFiltered(@Param("userId") Long userId,
                                          @Param("search") String search,
                                          Pageable pageable);
}

