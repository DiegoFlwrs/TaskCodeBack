package com.flores.taskcodeback.task.repository;

import com.flores.taskcodeback.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByUserIdOrderByFechaDescCreatedAtDesc(Long userId);

    List<Task> findByUserIdAndFechaOrderByCreatedAtDesc(Long userId, LocalDate fecha);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.fecha BETWEEN :start AND :end ORDER BY t.fecha DESC, t.createdAt DESC")
    List<Task> findByUserIdAndFechaBetween(@Param("userId") Long userId,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end);
}

