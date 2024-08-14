package GInternational.server.api.repository;

import GInternational.server.api.entity.CheckAttendance;
import GInternational.server.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckAttendanceRepository extends JpaRepository<CheckAttendance, Long> {

    Optional<CheckAttendance> findByUserAndAttendanceDate(User user, LocalDateTime date);

    long countByUserAndAttendanceDateBetween(User user, LocalDateTime startDate, LocalDateTime endDate);

    List<CheckAttendance> findByUserAndAttendanceDateBetween(User user, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT ca FROM attendance ca JOIN ca.user u WHERE " +
            "(:username IS NULL OR u.username = :username) AND " +
            "(:nickname IS NULL OR u.nickname = :nickname) AND " +
            "(:startDate IS NULL OR ca.attendanceDate >= :startDate) AND " +
            "(:endDate IS NULL OR ca.attendanceDate <= :endDate) " +
            "ORDER BY ca.attendanceDate DESC")
    List<CheckAttendance> findByCriteria(
            @Param("username") String username,
            @Param("nickname") String nickname,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
