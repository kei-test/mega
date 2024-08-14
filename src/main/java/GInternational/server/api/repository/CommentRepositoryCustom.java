package GInternational.server.api.repository;

import GInternational.server.api.dto.CommentResDTO;
import GInternational.server.api.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CommentRepositoryCustom {

    Page<CommentResDTO> findByArticleParentAndChild(Long id, Pageable pageable);

    Optional<Comment> findArticleCommentByIdWithParent(Long id);
    Long getTotalCommentCountByArticleId(Long id);
}
