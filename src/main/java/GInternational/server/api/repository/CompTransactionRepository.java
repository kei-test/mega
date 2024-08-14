package GInternational.server.api.repository;

import GInternational.server.api.entity.CompTransaction;
import GInternational.server.api.entity.User;
import GInternational.server.api.vo.RollingTransactionEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CompTransactionRepository extends JpaRepository<CompTransaction, Long>, JpaSpecificationExecutor<CompTransaction> {

    boolean existsByUserAndCreatedAtGreaterThanEqual(User user, LocalDateTime createdAt);

    List<CompTransaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT SUM(r.calculatedReward) " +
            "FROM comp_transaction r " +
            "JOIN r.user u " +
            "WHERE FUNCTION('MONTH', r.processedAt) = :month " +
            "AND FUNCTION('YEAR', r.processedAt) = :year " +
            "AND u.role = 'ROLE_USER'")
    Long sumCompPointsByMonthAndYearForRoleUser(@Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(rt.calculatedReward) " +
            "FROM comp_transaction rt " +
            "JOIN rt.user u " +
            "WHERE rt.processedAt BETWEEN :startOfDay AND :endOfDay " +
            "AND u.role = 'ROLE_USER'")
    Long getCompPointByDateForRoleUser(@Param("startOfDay") LocalDateTime startOfDay,
                                       @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT COUNT(DISTINCT FUNCTION('DAY', rt.processedAt)) " +
            "FROM comp_transaction rt " +
            "WHERE FUNCTION('MONTH', rt.processedAt) = :month AND FUNCTION('YEAR', rt.processedAt) = :year")
    int countDaysWithCompPoints(@Param("month") int month, @Param("year") int year);

    Long countByStatus(RollingTransactionEnum status);
}
