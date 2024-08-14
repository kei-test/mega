package GInternational.server.api.repository;

import GInternational.server.api.entity.Template;
import GInternational.server.api.vo.TemplateTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    @Query("SELECT MAX(t.turn) FROM template t")
    Optional<Integer> findMaxTurn();

    List<Template> findAllByOrderByTurnAsc();

    // type 별로 필터링하고 turn에 따라 오름차순으로 정렬하여 모든 템플릿 조회
    List<Template> findAllByTypeOrderByTurnAsc(TemplateTypeEnum type);
}
