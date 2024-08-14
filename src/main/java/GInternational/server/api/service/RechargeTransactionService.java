package GInternational.server.api.service;

import GInternational.server.api.dto.RechargeTransactionApprovedDTO;
import GInternational.server.api.dto.RechargeTransactionResDTO;
import GInternational.server.api.dto.RechargeTransactionsSummaryDTO;
import GInternational.server.api.dto.WalletDetailDTO;
import GInternational.server.api.entity.RechargeTransaction;
import GInternational.server.api.repository.RechargeTransactionRepository;
import GInternational.server.api.vo.TransactionEnum;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(value = "clientServerTransactionManager")
@RequiredArgsConstructor
public class RechargeTransactionService {

    private final RechargeTransactionRepository rechargeTransactionRepository;

    /**
     * 특정 사용자의 충전 거래를 페이징하여 조회.
     *
     * @param userId           사용자 ID
     * @param page             페이지 번호
     * @param size             페이지 크기
     * @param principalDetails 현재 사용자의 인증 정보
     * @return 충전 거래 내역과 페이징 정보
     */
    public Page<RechargeTransaction> getTransactionsByUserId(Long userId, int page, int size, PrincipalDetails principalDetails) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("userId").descending());
        Page<RechargeTransaction> transactions = rechargeTransactionRepository.findByUserIdAndTransaction(userId, pageable);
        long totalElements = rechargeTransactionRepository.countByUserId(userId);
        return new PageImpl<>(transactions.getContent(),pageable,totalElements);
    }

    /**
     * 특정 기간과 상태에 따른 충전 거래를 조회합니다.
     *
     * @param startDateTime     시작일
     * @param endDateTime       종료일
     * @param status            거래 상태
     * @param principalDetails  현재 사용자의 인증 정보
     * @return 조회된 충전 거래 목록
     */
    @Transactional(value = "clientServerTransactionManager",readOnly = true)
    public List<RechargeTransaction> findAllByProcessedAtBetweenAndStatus(LocalDateTime startDateTime, LocalDateTime endDateTime, TransactionEnum status, PrincipalDetails principalDetails) {
        return rechargeTransactionRepository.findAllByProcessedAtBetweenAndStatus(startDateTime, endDateTime, status);
    }

    /**
     * 특정 기간과 상태에 따른 충전 거래를 조회하고 DTO 리스트로 변환.
     *
     * @param startDateTime     시작일
     * @param endDateTime       종료일
     * @param status            거래 상태
     * @param principalDetails  현재 사용자의 인증 정보
     * @return 조회된 충전 거래 DTO 목록
     */
    public List<RechargeTransactionResDTO> findAllTransactionsByCreatedAtBetweenDatesWithStatusDTO(
            LocalDateTime startDateTime, LocalDateTime endDateTime, Optional<TransactionEnum> status, PrincipalDetails principalDetails) {

        List<RechargeTransaction> rechargeTransactions = status
                .map(s -> rechargeTransactionRepository.findAllByCreatedAtBetweenAndStatus(startDateTime, endDateTime, s))
                .orElse(rechargeTransactionRepository.findAllByCreatedAtBetween(startDateTime, endDateTime));

        return rechargeTransactions.stream()
                .map(rt -> new RechargeTransactionResDTO(
                        rt.getId(),
                        rt.getUser().getId(),
                        rt.getUsername(),
                        rt.getNickname(),
                        rt.getPhone(),
                        rt.getGubun(),
                        rt.getRechargeAmount(),
                        rt.getRemainingSportsBalance(),
                        rt.getBonus(),
                        rt.getRemainingPoint(),
                        rt.getMessage(),
                        rt.getStatus(),
                        rt.getIp(),
                        rt.getSite(),
                        rt.getUser().getWallet() != null ? new WalletDetailDTO(rt.getUser().getWallet()) : null,
                        rt.getUser().getLv(),
                        rt.getUser().getDistributor(),
                        rt.getUser().getStore(),
                        rt.getCreatedAt(),
                        rt.getProcessedAt()))
                .collect(Collectors.toList());
    }

    public List<RechargeTransaction> findAllTransactionsByCreatedAtBetweenDates(LocalDateTime startDateTime, LocalDateTime endDateTime, PrincipalDetails principalDetails) {
        return rechargeTransactionRepository.findAllByCreatedAtBetween(startDateTime, endDateTime);
    }

    /**
     * 특정 기간에 충전 거래를 조회.
     *
     * @param startDate         시작일
     * @param endDate           종료일
     * @param principalDetails  현재 사용자의 인증 정보
     * @return 조회된 충전 거래 목록
     */
    public List<RechargeTransaction> rechargedSettlement(LocalDate startDate, LocalDate endDate, PrincipalDetails principalDetails) {

        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<RechargeTransaction> rechargeTransactions = rechargeTransactionRepository.findByStatusAndProcessedAtBetween(
                TransactionEnum.APPROVAL,
                startDateTime,
                endDateTime);

        return rechargeTransactions;
    }

    /**
     * 승인된 거래를 페이지네이션하여 조회.
     *
     * @param userId            사용자 ID
     * @param startDateTime     시작 일시
     * @param endDateTime       종료 일시
     * @param page              페이지 번호
     * @param size              페이지 크기
     * @param principalDetails  현재 사용자의 인증 정보
     * @return 페이지네이션된 승인된 거래 목록
     */
    public Page<RechargeTransactionApprovedDTO> getApprovedTransactionsWithPagination(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime, int page, int size, PrincipalDetails principalDetails) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        return rechargeTransactionRepository.findByUserIdAndStatusAndProcessedAtBetween(
                        userId, TransactionEnum.APPROVAL, startDateTime, endDateTime, pageable)
                .map(transaction -> new RechargeTransactionApprovedDTO(transaction.getId(), transaction.getRechargeAmount(), transaction.getProcessedAt()));
    }

    /**
     * 특정 기간의 충전 거래에 대한 총 충전금액과 평균 충전금액, 그리고 사용자의 모든 충전금액의 합을 조회.
     *
     * @param userId            사용자 ID
     * @param startDateTime     시작 일시
     * @param endDateTime       종료 일시
     * @param principalDetails  현재 사용자의 인증 정보
     * @return 충전 거래 요약 정보
     */
    public RechargeTransactionsSummaryDTO getTransactionsSummary(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime, PrincipalDetails principalDetails) {
        List<RechargeTransaction> approvedRechargeTransactions = rechargeTransactionRepository.findByUserIdAndStatusAndProcessedAtBetween(
                userId, TransactionEnum.APPROVAL, startDateTime, endDateTime);

        long totalRechargeAmount = approvedRechargeTransactions.stream()
                .mapToLong(RechargeTransaction::getRechargeAmount)
                .sum();

        BigDecimal averageRechargeAmount = approvedRechargeTransactions.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf(approvedRechargeTransactions.stream()
                        .mapToLong(RechargeTransaction::getRechargeAmount)
                        .average()
                        .orElse(0));

        long totalAllTimeRechargeAmount = rechargeTransactionRepository.findByUserIdAndStatus(userId, TransactionEnum.APPROVAL).stream()
                .mapToLong(RechargeTransaction::getRechargeAmount)
                .sum();

        return new RechargeTransactionsSummaryDTO(totalRechargeAmount, averageRechargeAmount, totalAllTimeRechargeAmount);
    }
}


