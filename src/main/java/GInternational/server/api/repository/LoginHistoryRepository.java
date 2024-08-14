package GInternational.server.api.repository;


import GInternational.server.api.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    // 특정 날짜 범위의 로그인 이력 조회
    List<LoginHistory> findByAttemptDateBetween(LocalDateTime start, LocalDateTime end);
}
