package GInternational.server.kplay.debit.repository;

import GInternational.server.kplay.debit.entity.Debit;
import GInternational.server.api.dto.MonthlyBetStatisticDailyAmountDTO;
import GInternational.server.api.domain.UserSlotStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DebitRepository extends JpaRepository<Debit, Long>, DebitCustomRepository {

    Optional<Debit> findByTxnId(String txnId);

    List<Debit> findAll();

    @Query("SELECT d FROM debit d WHERE d.user_id = :aasId")
    List<Debit> findAllByUserId(@Param("aasId") int aasId);


    @Query("SELECT d FROM debit d WHERE d.user_id = :aasId AND d.createdAt >= :startDateTime AND d.createdAt <= :endDateTime")
    List<Debit> findAllByUserIdAndCreatedDateBetween(@Param("aasId") int aasId, @Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT d FROM debit d WHERE d.user_id IN :userIds AND d.createdAt >= :startDateTime AND d.createdAt <= :endDateTime")
    List<Debit> findAllByUserIdsAndCreatedDateBetween(@Param("userIds") List<Integer> userIds,
                                                      @Param("startDateTime") LocalDateTime startDateTime,
                                                      @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT d.user_id AS userId, SUM(d.amount + d.credit_amount) AS totalBet, SUM(c.amount) AS totalWin " +
            "FROM debit d JOIN credit c ON d.id = c.debit.id " +
            "WHERE d.createdAt BETWEEN :start AND :end AND d.prd_id IN :validPrdIds " +
            "GROUP BY d.user_id ORDER BY SUM(d.amount + d.credit_amount) DESC")
    List<UserSlotStats> findSlotStatistics(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end,
                                           @Param("validPrdIds") List<Integer> validPrdIds);

    Page<Debit> findByCreatedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime, Pageable pageable);

}
