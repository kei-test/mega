package GInternational.server.api.service;

import GInternational.server.api.dto.*;
import GInternational.server.api.entity.ExchangeTransaction;
import GInternational.server.api.entity.Wallet;
import GInternational.server.api.repository.ExchangeRepository;
import GInternational.server.api.repository.WalletRepository;
import GInternational.server.api.vo.TransactionEnum;
import GInternational.server.common.exception.ExceptionCode;
import GInternational.server.common.exception.RestControllerException;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(value = "clientServerTransactionManager")
@RequiredArgsConstructor
public class ExchangeTransactionService {

    private final ExchangeRepository exchangeRepository;
    private final WalletRepository walletRepository;

    /**
     * 특정 사용자의 환전 거래 내역을 페이지별로 조회.
     *
     * @param userId           사용자 ID
     * @param page             페이지 번호
     * @param size             페이지 크기
     * @param principalDetails 현재 사용자의 인증 정보
     * @return 페이지별 환전 거래 내역
     */
    public Page<ExchangeTransactionResponseDTO> getExchangeTransactionsByUserId(Long userId, int page, int size, PrincipalDetails principalDetails) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("userId").descending());
        Page<ExchangeTransaction> transactions = exchangeRepository.findByUserIdAndExchangeTransaction(userId, pageable);

        List<ExchangeTransactionResponseDTO> dtoList = transactions.getContent().stream().map(transaction -> {
            ExchangeTransactionResponseDTO dto = new ExchangeTransactionResponseDTO();
            dto.setId(transaction.getId());
            dto.setUserId(transaction.getUser().getId());
            dto.setUsername(transaction.getUser().getUsername());
            dto.setNickname(transaction.getUser().getNickname());
            dto.setGubun(transaction.getGubun());
            dto.setExchangeAmount(transaction.getExchangeAmount());
            dto.setBonus(transaction.getBonus());
            dto.setRemainingSportsBalance(transaction.getRemainingSportsBalance());
            dto.setStatus(transaction.getStatus());
            dto.setIp(transaction.getIp());
            dto.setSite(transaction.getSite());
            dto.setCreatedAt(transaction.getCreatedAt());
            dto.setProcessedAt(transaction.getProcessedAt());
            WalletDetailDTO walletDetail = new WalletDetailDTO(transaction.getWallet());
            dto.setWallet(walletDetail);
            return dto;
        }).collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, transactions.getTotalElements());
    }

    /**
     * 주어진 기간 및 상태에 따라 환전 거래를 조회.
     *
     * @param status           거래 상태
     * @param startDateTime    조회 시작 일시
     * @param endDateTime      조회 종료 일시
     * @param principalDetails 현재 사용자의 인증 정보
     * @return 조회된 환전 거래 목록
     */
    @Transactional(value = "clientServerTransactionManager",readOnly = true)
    public List<ExchangeTransaction> findByStatusAndProcessedAtBetween(TransactionEnum status, LocalDateTime startDateTime, LocalDateTime endDateTime, PrincipalDetails principalDetails) {
        List<ExchangeTransaction> transactions = exchangeRepository.findByStatusAndProcessedAtBetween(status, startDateTime, endDateTime);

        transactions.forEach(transaction -> {
            transaction.getWallet().getId();
            Wallet wallet = walletRepository.findByIdWithUser(transaction.getWallet().getId())
                    .orElseThrow(() -> new RestControllerException(ExceptionCode.WALLET_NOT_FOUND, "사용자 지갑을 찾을 수 없습니다."));
        });

        return transactions;
    }

    /**
     * 주어진 기간과 상태에 해당하는 생성된 날짜 사이의 모든 환전 거래를 조회하고 DTO 리스트로 직접 변환합니다.
     *
     * @param startDateTime    조회 시작 일시
     * @param endDateTime      조회 종료 일시
     * @param status           거래 상태
     * @param principalDetails 현재 사용자의 인증 정보
     * @return 주어진 조건에 해당하는 환전 거래 DTO 목록
     */
    public List<ExchangeTransactionResponseDTO> findAllTransactionsByCreatedAtBetweenDatesWithStatusDTO(LocalDateTime startDateTime, LocalDateTime endDateTime, Optional<TransactionEnum> status, PrincipalDetails principalDetails) {
        List<ExchangeTransaction> transactions = status.isPresent() ?
                exchangeRepository.findAllByCreatedAtBetweenAndStatus(startDateTime, endDateTime, status.get()) :
                exchangeRepository.findAllByCreatedAtBetween(startDateTime, endDateTime);

        return transactions.stream().map(tx -> {
            Wallet wallet = walletRepository.findById(tx.getWallet().getId()).orElseThrow(() ->
                    new RestControllerException(ExceptionCode.WALLET_NOT_FOUND, "사용자 지갑을 찾을 수 없습니다."));
            return new ExchangeTransactionResponseDTO(
                    tx.getId(),
                    tx.getLv(),
                    tx.getUser().getId(),
                    tx.getUsername(),
                    tx.getNickname(),
                    tx.getPhone(),
                    tx.getGubun(),
                    tx.getExchangeAmount(),
                    tx.getBonus(),
                    tx.getRemainingSportsBalance(),
                    tx.getStatus(),
                    tx.getIp(),
                    tx.getSite(),
                    new WalletDetailDTO(wallet),
                    tx.getUser().getDistributor(),
                    tx.getUser().getStore(),
                    tx.getCreatedAt(),
                    tx.getProcessedAt());
        }).collect(Collectors.toList());
    }

    /**
     * 주어진 기간에 해당하는 환전 정산을 수행.
     *
     * @param startDate        조회 시작 날짜
     * @param endDate          조회 종료 날짜
     * @param principalDetails 현재 사용자의 인증 정보
     * @return 주어진 기간에 수행된 환전 거래 목록
     */
    public List<ExchangeSettlementAdminDTO> exchangedSettlement(LocalDate startDate, LocalDate endDate, PrincipalDetails principalDetails) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<ExchangeTransaction> rsList = exchangeRepository.findByExchangeTransaction(startDate, endDate);
        List<ExchangeSettlementAdminDTO> dtoList = rsList.stream().map(transaction -> {
            ExchangeSettlementAdminDTO dto = new ExchangeSettlementAdminDTO();
            dto.setExchangedCount(transaction.getExchangedCount());
            BeanUtils.copyProperties(transaction, dto);
            return dto;
        }).collect(Collectors.toList());
        return dtoList;
    }

    /**
     * 특정 사용자의 환전 거래 중 승인된 거래를 페이지별로 조회.
     *
     * @param userId           사용자 ID
     * @param startDateTime    조회 시작 일시
     * @param endDateTime      조회 종료 일시
     * @param page             페이지 번호
     * @param size             페이지 크기
     * @param principalDetails 현재 사용자의 인증 정보
     * @return 페이지별 승인된 환전 거래 목록
     */
    public Page<ExchangeTransactionApprovedDTO> getApprovedTransactionsWithPagination(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime, int page, int size, PrincipalDetails principalDetails) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        return exchangeRepository.findByUserIdAndStatusAndProcessedAtBetween(
                        userId, TransactionEnum.APPROVAL, startDateTime, endDateTime, pageable)
                .map(ExchangeTransaction -> new ExchangeTransactionApprovedDTO(ExchangeTransaction.getId(), ExchangeTransaction.getExchangeAmount(), ExchangeTransaction.getProcessedAt()));
    }

    /**
     * 특정 사용자의 환전 거래 중 승인된 거래의 총 충전 금액과 평균 충전 금액을 조회.
     *
     * @param userId           사용자 ID
     * @param startDateTime    조회 시작 일시
     * @param endDateTime      조회 종료 일시
     * @param principalDetails 현재 사용자의 인증 정보
     * @return 승인된 거래의 총 충전 금액과 평균 충전 금액 정보
     */
    public ExchangeTransactionsSummaryDTO getTransactionsSummary(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime, PrincipalDetails principalDetails) {
        List<ExchangeTransaction> approvedTransactions = exchangeRepository.findByUserIdAndStatusAndProcessedAtBetween(
                userId, TransactionEnum.APPROVAL, startDateTime, endDateTime);

        long totalExchangeAmount = approvedTransactions.stream()
                .mapToLong(ExchangeTransaction::getExchangeAmount)
                .sum();

        BigDecimal averageExchangeAmount = approvedTransactions.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf(approvedTransactions.stream()
                        .mapToLong(ExchangeTransaction::getExchangeAmount)
                        .average()
                        .orElse(0));

        long totalAllTimeExchangeAmount = exchangeRepository.findByUserIdAndStatus(userId, TransactionEnum.APPROVAL).stream()
                .mapToLong(ExchangeTransaction::getExchangeAmount)
                .sum();

        return new ExchangeTransactionsSummaryDTO(totalExchangeAmount, averageExchangeAmount, totalAllTimeExchangeAmount);
    }
}
