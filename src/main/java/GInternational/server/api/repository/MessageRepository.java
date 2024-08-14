package GInternational.server.api.repository;

import GInternational.server.api.entity.Messages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Messages, Long>,MessageRepositoryCustom {

    Optional<Messages> findById(Long id);

    @Query("SELECT m FROM messages m WHERE m.createdAt >= :startDate AND m.createdAt <= :endDate")
    List<Messages> findAllByDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
