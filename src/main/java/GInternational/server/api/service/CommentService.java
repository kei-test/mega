package GInternational.server.api.service;

import GInternational.server.api.entity.Articles;
import GInternational.server.api.dto.CommentReqDTO;
import GInternational.server.api.dto.CommentResDTO;
import GInternational.server.api.entity.Comment;
import GInternational.server.api.mapper.CommentReqMapper;
import GInternational.server.api.mapper.CommentResMapper;
import GInternational.server.api.repository.ArticleRepository;
import GInternational.server.api.repository.CommentRepository;
import GInternational.server.api.vo.ExpRecordEnum;
import GInternational.server.common.exception.ExceptionCode;
import GInternational.server.common.exception.RestControllerException;
import GInternational.server.security.auth.PrincipalDetails;
import GInternational.server.api.entity.User;
import GInternational.server.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
@Transactional(value = "clientServerTransactionManager")
@RequiredArgsConstructor
public class CommentService {

    private final ArticleService articleService;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final CommentReqMapper commentReqMapper;
    private final CommentResMapper commentResMapper;
    private final ExpRecordService expRecordService;

    /**
     * 댓글 생성.
     * @param articleId 게시글 ID
     * @param commentReqDTO 댓글 요청 데이터
     * @param principalDetails 사용자 인증 정보
     * @return 생성된 댓글 정보
     */
    public CommentResDTO insertComment(Long articleId, CommentReqDTO commentReqDTO, PrincipalDetails principalDetails, HttpServletRequest request) {
        User user = userRepository.findById(principalDetails.getUser().getId()).orElseThrow
                (() -> new RestControllerException(ExceptionCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다."));
        if (!user.isCanComment()) {
            throw new RestControllerException(ExceptionCode.PERMISSION_DENIED, "댓글 작성이 불가능합니다. 관리자에게 문의하세요.");
        }
        Articles article = articleService.validateArticle(articleId);
        if (!article.isCommentAllowed()) {
            throw new RestControllerException(ExceptionCode.PERMISSION_DENIED, "이 게시글에는 댓글을 달 수 없습니다.");
        }

        String clientIp = request.getRemoteAddr();

        // 고객센터 카테고리 게시글에 대한 댓글 권한 확인
        if ("고객센터".equals(article.getCategory().getName()) &&
                !(user.getRole().equals("ROLE_ADMIN") || user.getRole().equals("ROLE_MANAGER"))) {
            throw new RestControllerException(ExceptionCode.PERMISSION_DENIED, "고객센터 게시글에는 관리자 또는 매니저만 댓글을 달 수 있습니다.");
        }

        Comment comment = commentReqMapper.toEntity(commentReqDTO);
        Comment parentComment;
        if (commentReqDTO.getParentId() != null) {
            parentComment = commentRepository.findByIdAndArticles(commentReqDTO.getParentId(),article).orElseThrow(
                    () -> new RestControllerException(ExceptionCode.DATA_NOT_FOUND, "PARENT_NOT_FOUND"));
            comment.updateParent(parentComment);
        }
        comment.updateWriter(user);
        comment.setArticles(article);
        comment.setCreatedAt(LocalDateTime.now());
        Comment savedComment = commentRepository.save(comment);

        // 고객센터 게시글 댓글 작성 시 답변 상태를 '답변완료'로 변경
        if ("고객센터".equals(article.getCategory().getName())) {
            article.setAnswerStatus("답변완료");
            articleRepository.save(article);
        }

        article.incrementCommentCount();
        articleRepository.save(article);

        expRecordService.recordDailyExpUpToFiveTime(user.getId(), user.getUsername(), user.getNickname(), 1, clientIp, ExpRecordEnum.댓글작성경험치);

        return commentResMapper.toDto(savedComment);
    }

    /**
     * 특정 게시글의 댓글 조회.
     * @param articlesId 게시글 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param principalDetails 사용자 인증 정보
     * @return 조회된 댓글 페이지 정보
     */
    public Page<CommentResDTO> getCommentsByArticlesId(Long articlesId, int page, int size, PrincipalDetails principalDetails) {
        Pageable pageable = PageRequest.of(page, size);
        return commentRepository.findByArticleParentAndChild(articlesId, pageable);
    }

    /**
     * 댓글 삭제.
     * @param commentId 댓글 ID
     * @param principalDetails 사용자 인증 정보
     */
    public void deleteComment(Long commentId, PrincipalDetails principalDetails) {
        Comment comment = commentRepository.findArticleCommentByIdWithParent(commentId).orElseThrow
                (() -> new RestControllerException(ExceptionCode.COMMENT_NOT_FOUND, "댓글을 찾을 수 없습니다."));
        User user = principalDetails.getUser();
        if (user == null) {
            throw new RestControllerException(ExceptionCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다.");
        }

        if (!comment.getWriter().getId().equals(user.getId()) && !user.getRole().equals("ROLE_MANAGER") && !user.getRole().equals("ROLE_ADMIN")) {
            throw new RestControllerException(ExceptionCode.UNAUTHORIZED_ACCESS, "댓글 삭제 권한이 없습니다.");
        }

        Articles article = comment.getArticles();

        if (comment.getParent() != null) {
            // 대댓글인 경우 부모 댓글의 자식 목록에서 해당 대댓글을 제거합니다.
            Comment parent = comment.getParent();
            parent.getChildren().remove(comment);
            commentRepository.save(parent);
            article.decrementCommentCount();
        } else {
            // 대댓글이 아닌 경우는 기존의 삭제 로직을 그대로 사용합니다.
            if (comment.getChildren().size() != 0) {
                comment.changeIsDeleted(true);
            } else {
                commentRepository.delete(getDeletableAncestorComment(comment));
                article.decrementCommentCount();
            }
        }

        articleRepository.save(article);
    }

    /**
     * 삭제 가능한 상위 댓글 조회.
     * @param comment 현재 댓글
     * @return 삭제할 상위 댓글
     */
    public Comment getDeletableAncestorComment(Comment comment) {
        Comment parent = comment.getParent(); // 현재 댓글의 부모를 구함
        if (parent != null && parent.getChildren().size() == 1 && parent.isDeleted())
            // 부모가 있고, 부모의 자식이 1개(지금 삭제하는 댓글)이고, 부모의 삭제 상태가 TRUE인 댓글이라면 재귀
            return getDeletableAncestorComment(parent);
        return comment; // 삭제해야하는 댓글 반환
    }

    /**
     * 사용자의 모든 댓글 조회.
     * @param userId 사용자 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param principalDetails 사용자 인증 정보
     * @return 조회된 댓글 페이지 정보
     */
    public Page<CommentResDTO> getMyComments(Long userId, int page, int size, PrincipalDetails principalDetails) {
        User user = userRepository.findById(userId).orElseThrow(()
                -> new RestControllerException(ExceptionCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다"));
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Comment> commentsPage = commentRepository.findByWriter(user, pageable);
        return commentsPage.map(commentResMapper::toDto);
    }
}
