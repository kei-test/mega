package GInternational.server.api.repository;

import GInternational.server.api.entity.AmazonMoney;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AmazonMoneyRepository extends JpaRepository<AmazonMoney, Long> {
    // 특정 사용자 ID 기반 조회
    List<AmazonMoney> findByUserId(Long userId);

    // 날짜 필터링 및 사용자 ID 기반 조회
    List<AmazonMoney> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    // 날짜 필터링을 통한 전체 조회
    List<AmazonMoney> findAllByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
