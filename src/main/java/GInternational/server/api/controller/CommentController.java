package GInternational.server.api.controller;

import GInternational.server.api.dto.CommentReqDTO;
import GInternational.server.api.dto.CommentResDTO;
import GInternational.server.api.service.CommentService;
import GInternational.server.common.dto.MultiResponseDto;
import GInternational.server.common.dto.SingleResponseDto;
import GInternational.server.common.exception.RestControllerException;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Positive;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 생성.
     * @param categoryId 카테고리 ID
     * @param articleId 게시글 ID
     * @param commentReqDTO 댓글 정보
     * @param authentication 사용자 인증 정보
     * @return 생성된 댓글 정보
     */
    @PostMapping("/users/{categoryId}/{articleId}/comment")
    public ResponseEntity insertComment(@PathVariable("categoryId") Long categoryId,
                                        @PathVariable("articleId") Long articleId,
                                        @RequestBody CommentReqDTO commentReqDTO,
                                        HttpServletRequest request,
                                        Authentication authentication) {
        try {
            PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
            CommentResDTO response = commentService.insertComment(articleId, commentReqDTO, principal, request);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("data", new SingleResponseDto<>(response));
            responseBody.put("message", "댓글이 생성되었습니다.");
            return new ResponseEntity<>(responseBody, HttpStatus.CREATED);
        } catch (RestControllerException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * 게시글의 모든 댓글 조회.
     * @param categoryId 카테고리 ID
     * @param articleId 게시글 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param authentication 사용자 인증 정보
     * @return 조회된 댓글 목록
     */
    @GetMapping("/users/{categoryId}/{articleId}/comments")
    public ResponseEntity<MultiResponseDto<CommentResDTO>> getCommentsByBoardId(@PathVariable("categoryId") Long categoryId,
                                                                                @PathVariable("articleId") @Positive Long articleId,
                                                                                @RequestParam(defaultValue = "1") int page,
                                                                                @RequestParam(defaultValue = "10") int size,
                                                                                Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        Page<CommentResDTO> commentPage = commentService.getCommentsByArticlesId(articleId, page - 1, size, principal);
        MultiResponseDto<CommentResDTO> responseDto = new MultiResponseDto<>(commentPage.getContent(), commentPage);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    /**
     * 댓글 삭제.
     * @param categoryId 카테고리 ID
     * @param articleId 게시글 ID
     * @param commentId 댓글 ID
     * @param authentication 사용자 인증 정보
     */
    @DeleteMapping("/users/{categoryId}/{articleId}/{commentId}")
    public ResponseEntity deleteComment(@PathVariable("categoryId") Long categoryId,
                                        @PathVariable("articleId") @Positive Long articleId,
                                        @PathVariable("commentId") @Positive Long commentId,
                                        Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        commentService.deleteComment(commentId,principal);
        return ResponseEntity.ok("댓글이 삭제 되었습니다");
    }

    /**
     * 사용자의 모든 댓글 조회.
     * @param principalDetails 사용자 인증 정보
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param authentication 사용자 인증 정보
     * @return 조회된 사용자 댓글 목록
     */
    @GetMapping("/users/my-comments")
    public ResponseEntity<MultiResponseDto<CommentResDTO>> getMyComments(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                         @RequestParam(defaultValue = "1") int page,
                                                                         @RequestParam(defaultValue = "10") int size,
                                                                         Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        Page<CommentResDTO> commentsPage = commentService.getMyComments(principalDetails.getUser().getId(), page - 1, size, principal);
        MultiResponseDto<CommentResDTO> response = new MultiResponseDto<>(commentsPage.getContent(), commentsPage);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
