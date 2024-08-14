package GInternational.server.api.repository;

import GInternational.server.api.entity.Ip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IpRepository extends JpaRepository<Ip,Long> {

    Optional<Ip> findById(Long id);
    Ip findByIpContent(String ipContent);
    Page<Ip> findAllByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

}
