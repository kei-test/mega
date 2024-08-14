package GInternational.server.api.repository;

import GInternational.server.api.dto.LoginInquiryListDTO;
import GInternational.server.api.entity.Articles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Articles, Long>, ArticleRepositoryCustom{

    Optional<Articles> findById(Long ArticleId);

    Optional<Articles> findByIdAndViewStatus(Long articleId, String viewStatus);

    // 관리자와 매니저용: viewStatus 상관없이 모든 게시물 조회
    List<Articles> findByIsTopTrueOrderByCreatedAtDesc();

    // 일반 사용자용: viewStatus가 "노출"인 게시물만 조회
    @Query("SELECT a FROM articles a WHERE a.isTop = true AND a.viewStatus = :viewStatus ORDER BY a.createdAt DESC")
    List<Articles> findByIsTopTrueAndViewStatusOrderByCreatedAtDesc(@Param("viewStatus") String viewStatus);

//    @Query("SELECT a FROM articles a WHERE " +
//            "(:title IS NULL OR a.title LIKE %:title%) AND " +
//            "(:content IS NULL OR a.content LIKE %:content%) AND " +
//            "(:nickname IS NULL OR a.writer.nickname LIKE %:nickname%) AND " +
//            "(a.category.name = :categoryName) AND " +
//            "(:viewStatus IS NULL OR a.viewStatus = :viewStatus) AND " +
//            "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
//            "(:endDate IS NULL OR a.createdAt <= :endDate)")
//    List<Articles> searchByAdvancedCriteria(
//            @Param("title") String title,
//            @Param("content") String content,
//            @Param("nickname") String nickname,
//            @Param("viewStatus") String viewStatus,
//            @Param("categoryName") String categoryName,
//            @Param("startDate") LocalDateTime startDate,
//            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM articles a WHERE a.category.id = :categoryId AND a.id > :articleId ORDER BY a.id ASC")
    List<Articles> findByCategoryAndIdGreaterThanOrderByIdAsc(@Param("categoryId") Long categoryId, @Param("articleId") Long articleId);

    @Query("SELECT a FROM articles a WHERE a.category.id = :categoryId AND a.id < :articleId ORDER BY a.id DESC")
    List<Articles> findByCategoryAndIdLessThanOrderByIdDesc(@Param("categoryId") Long categoryId, @Param("articleId") Long articleId);

    @Query("SELECT a FROM articles a WHERE a.category.id = :categoryId AND a.id > :articleId AND a.viewStatus = :viewStatus ORDER BY a.id ASC")
    List<Articles> findByCategoryAndIdGreaterThanAndViewStatusOrderByIdAsc(@Param("categoryId") Long categoryId, @Param("articleId") Long articleId, String viewStatus);

    @Query("SELECT a FROM articles a WHERE a.category.id = :categoryId AND a.id < :articleId AND a.viewStatus = :viewStatus ORDER BY a.id DESC")
    List<Articles> findByCategoryAndIdLessThanAndViewStatusOrderByIdDesc(@Param("categoryId") Long categoryId, @Param("articleId") Long articleId, String viewStatus);

    @Query("SELECT a FROM articles a JOIN a.writer u WHERE a.category.id = :categoryId AND FUNCTION('MONTH', a.createdAt) = :month AND FUNCTION('YEAR', a.createdAt) = :year AND u.role = 'ROLE_USER'")
    List<Articles> findByCategoryIdAndMonthAndYearAndUserRole(@Param("categoryId") Long categoryId, @Param("month") int month, @Param("year") int year);


    // 기존의 쿼리 메서드
    @Query("SELECT new GInternational.server.api.dto.LoginInquiryListDTO(a.writerName, a.ownerName, a.phone, a.ip, a.answerStatus, a.createdAt) " +
            "FROM articles a WHERE a.category.id = :categoryId AND a.title LIKE '로그인 문의%' " +
            "AND (:writerName IS NULL OR a.writerName = :writerName) " +
            "AND (:ownerName IS NULL OR a.ownerName = :ownerName) " +
            "AND (:phone IS NULL OR a.phone = :phone) " +
            "AND (:ip IS NULL OR a.ip = :ip) " +
            "ORDER BY a.createdAt DESC")
    List<LoginInquiryListDTO> findLoginInquiriesByFilters(@Param("categoryId") Long categoryId,
                                                          @Param("writerName") String writerName,
                                                          @Param("ownerName") String ownerName,
                                                          @Param("phone") String phone,
                                                          @Param("ip") String ip);

    @Query("SELECT a FROM articles a WHERE a.category.name = :categoryName")
    List<Articles> findAllByCategoryName(@Param("categoryName") String categoryName);

    List<Articles> findAllByCategoryNameAndViewStatus(String categoryName, String viewStatus);

    @Query("SELECT COUNT(a) FROM articles a WHERE a.answerStatus IN (?1)")
    Long countByAnswerStatuses(List<String> statuses);

    int countByAnswerStatus(String answerStatus);
}
