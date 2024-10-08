package GInternational.server.api.service;

import GInternational.server.api.entity.CouponTransaction;
import GInternational.server.api.repository.CouponTransactionRepository;
import GInternational.server.api.vo.CouponTransactionEnum;
import GInternational.server.api.utilities.AuditContext;
import GInternational.server.api.utilities.AuditContextHolder;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(value = "clientServerTransactionManager")
@RequiredArgsConstructor
public class CouponTransactionService {

    private final CouponTransactionRepository couponTransactionRepository;

    /**
     * 특정 사용자의 특정 기간 동안의 쿠폰 트랜잭션을 조회.
     *
     * @param userId    사용자 ID
     * @param startDate 조회 시작 날짜
     * @param endDate   조회 종료 날짜
     * @return 특정 사용자의 특정 기간 동안의 쿠폰 트랜잭션 목록
     */

    public List<CouponTransaction> getCouponTransactionsByUserIdAndDate(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime;

        if (endDate.equals(LocalDate.now())) {
            endDateTime = LocalDateTime.now();
        } else {
            endDateTime = endDate.plusDays(1).atStartOfDay();
        }
        return couponTransactionRepository.findByUserIdAndDate(userId, startDateTime, endDateTime);
    }

    /**
     * 특정 기간 동안의 모든 쿠폰 트랜잭션을 조회.
     *
     * @param startDate 조회 시작 날짜
     * @param endDate   조회 종료 날짜
     * @return 특정 기간 동안의 모든 쿠폰 트랜잭션 목록
     */

    @Transactional(value = "clientServerTransactionManager",readOnly = true)
    public List<CouponTransaction> findAllCouponTransactionByDate(LocalDate startDate, LocalDate endDate, PrincipalDetails principalDetails) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime;

        if (endDate.equals(LocalDate.now())) {
            endDateTime = LocalDateTime.now();
        } else {
            endDateTime = endDate.plusDays(1).atStartOfDay();
        }
        return couponTransactionRepository.findByCreatedAtBetween(startDateTime, endDateTime);
    }

    /**
     * 특정 상태와 기간 동안의 쿠폰 트랜잭션을 조회.
     *
     * @param status    쿠폰 트랜잭션 상태
     * @param startDate 조회 시작 날짜
     * @param endDate   조회 종료 날짜
     * @return 특정 상태와 기간 동안의 쿠폰 트랜잭션 목록
     */
    @Transactional(value = "clientServerTransactionManager",readOnly = true)
    public List<CouponTransaction> getCouponTransactionsByStatusAndDate(CouponTransactionEnum status, LocalDate startDate, LocalDate endDate, PrincipalDetails principalDetails) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime;

        if (endDate.equals(LocalDate.now())) {
            endDateTime = LocalDateTime.now();
        } else {
            endDateTime = endDate.plusDays(1).atStartOfDay();
        }
        return couponTransactionRepository.findByStatusAndCreatedAtBetween(status, startDateTime, endDateTime);
    }

    /**
     * 모든 쿠폰 트랜잭션을 조회.
     *
     * @param principalDetails 현재 사용자의 인증 정보
     * @return 모든 쿠폰 트랜잭션 목록
     */
    @Transactional(value = "clientServerTransactionManager", readOnly = true)
    public List<CouponTransaction> findAllCouponTransactions(PrincipalDetails principalDetails, String username, String nickname) {
        if (username != null && nickname != null) {
            return couponTransactionRepository.findByUsernameOrNickname(username, nickname);
        } else {
            return couponTransactionRepository.findAll();
        }
    }

    /**
     * 쿠폰 트랜잭션 상태를 업데이트.
     *
     * @param transactionId     트랜잭션 ID
     * @param newStatus         새로운 상태
     * @param principalDetails  현재 사용자의 인증 정보
     */
    @AuditLogService.Audit("머니쿠폰/행운복권 상태값 업데이트")
    public void updateCouponTransactionStatus(Long transactionId, CouponTransactionEnum newStatus, PrincipalDetails principalDetails, HttpServletRequest request) {
        CouponTransaction transaction = couponTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("쿠폰 거래를 찾을 수 없습니다."));

        transaction.setStatus(newStatus);

        AuditContext context = AuditContextHolder.getContext();
        String clientIp = request.getRemoteAddr();
        context.setIp(clientIp);
        context.setTargetId(String.valueOf(transaction.getUser().getId()));
        context.setUsername(transaction.getUsername());
        context.setDetails(transaction.getUsername() + "의 쿠폰 상태값 " + transaction.getStatus() + "로 변경");
        context.setAdminUsername(principalDetails.getUsername());
        context.setTimestamp(LocalDateTime.now());

        couponTransactionRepository.save(transaction);
    }
}
