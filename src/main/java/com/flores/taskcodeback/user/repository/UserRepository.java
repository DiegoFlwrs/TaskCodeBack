
package com.flores.taskcodeback.user.repository;

import com.flores.taskcodeback.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByEquipoIdAndActivoTrue(Long equipoId);

    List<User> findByIsIndependentTrueAndActivoTrue();

    @Query("SELECT u FROM User u WHERE u.createdBy.id = :leaderId AND u.activo = true")
    List<User> findByCreatedByIdAndActivoTrue(@Param("leaderId") Long leaderId);

    long countByEquipoIdAndActivoTrue(Long equipoId);

    long countByIsIndependentTrueAndActivoTrue();

    /** Limpia la referencia createdBy antes de eliminar un usuario */
    @Modifying
    @Query("UPDATE User u SET u.createdBy = null WHERE u.createdBy.id = :userId")
    void clearCreatedBy(@Param("userId") Long userId);
}