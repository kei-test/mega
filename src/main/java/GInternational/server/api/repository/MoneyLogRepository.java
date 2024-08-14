package GInternational.server.api.repository;

import GInternational.server.api.vo.MoneyLogCategoryEnum;
import GInternational.server.api.entity.MoneyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MoneyLogRepository extends JpaRepository<MoneyLog, Long> {

    // 날짜 범위와 카테고리를 기준으로 머니 적립 내역을 조회
    List<MoneyLog> findByCreatedAtBetweenAndCategory(LocalDateTime startDateTime, LocalDateTime endDateTime, MoneyLogCategoryEnum category);

    // 날짜 범위를 기준으로 머니 적립 내역을 조회 (카테고리 없음)
    List<MoneyLog> findByCreatedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    List<MoneyLog> findByUserIdAndCreatedAtBetweenAndCategory(Long userId, LocalDateTime start, LocalDateTime end, MoneyLogCategoryEnum category);

    List<MoneyLog> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}