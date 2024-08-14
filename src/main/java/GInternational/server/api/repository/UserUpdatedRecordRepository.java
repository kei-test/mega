package GInternational.server.api.repository;

import GInternational.server.api.entity.UserUpdatedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserUpdatedRecordRepository extends JpaRepository<UserUpdatedRecord, Long>, JpaSpecificationExecutor<UserUpdatedRecord> {
}
