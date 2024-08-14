package GInternational.server.api.repository;

import GInternational.server.api.entity.AdminLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminLoginHistoryRepository extends JpaRepository<AdminLoginHistory, Long> {

    List<AdminLoginHistory> findAllByOrderByAttemptDateDesc();

    List<AdminLoginHistory> findByLoginResult(String loginResult);
}
