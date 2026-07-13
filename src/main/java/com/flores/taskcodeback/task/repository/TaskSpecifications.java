package com.flores.taskcodeback.task.repository;

import com.flores.taskcodeback.task.entity.Task;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> forUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Task> withFecha(LocalDate fecha) {
        return (root, query, cb) -> cb.equal(root.get("fecha"), fecha);
    }

    public static Specification<Task> withFechaFrom(LocalDate fechaInicio) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), fechaInicio);
    }

    public static Specification<Task> withFechaTo(LocalDate fechaFin) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), fechaFin);
    }

    public static Specification<Task> withRqTicket(String rqTicket) {
        return (root, query, cb) -> cb.equal(root.get("rqTicket"), rqTicket);
    }

    public static Specification<Task> withAplicacion(String aplicacion) {
        return (root, query, cb) -> cb.equal(root.get("aplicacion"), aplicacion);
    }

    public static Specification<Task> withSearch(String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nombre")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("rqTicket"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("aplicacion"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("observacion"), "")), pattern)
        );
    }
}
