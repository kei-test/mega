package GInternational.server.api.controller;

import GInternational.server.api.dto.ExchangeSettlementAdminDTO;
import GInternational.server.api.dto.ExchangeTransactionResponseDTO;
import GInternational.server.common.dto.MultiResponseDto;
import GInternational.server.api.dto.ExchangeTransactionApprovedDTO;
import GInternational.server.api.dto.ExchangeTransactionsSummaryDTO;
import GInternational.server.api.entity.ExchangeTransaction;
import GInternational.server.api.mapper.ExchangeTransactionAdminResponseMapper;
import GInternational.server.api.mapper.ExchangeSettlementAdminMapper;
import GInternational.server.api.mapper.ExchangeTransactionResponseMapper;
import GInternational.server.api.service.ExchangeTransactionService;
import GInternational.server.api.vo.TransactionEnum;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class ExchangeTransactionController {


    private final ExchangeTransactionService exchangeTransactionService;
    private final ExchangeTransactionResponseMapper mapper;
    private final ExchangeTransactionAdminResponseMapper exchangeTransactionAdminResponseMapper;
    private final ExchangeSettlementAdminMapper exchangeSettlementAdminMapper;

    /**
     * 특정 사용자의 환전 거래 내역을 조회.
     *
     * @param userId          사용자 ID
     * @param page            페이지 번호
     * @param size            페이지 크기
     * @param authentication 인증 정보
     * @return 환전 거래 내역과 페이징 정보
     */
    @GetMapping("/users/{userId}/exchange/transactions")
    public ResponseEntity<MultiResponseDto<ExchangeTransactionResponseDTO>> getTransaction(@PathVariable("userId") @Positive Long userId,
                                                                                           @RequestParam int page,
                                                                                           @RequestParam int size,
                                                                                           Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        Page<ExchangeTransactionResponseDTO> transactionPage = exchangeTransactionService.getExchangeTransactionsByUserId(userId, page, size, principal);

        MultiResponseDto<ExchangeTransactionResponseDTO> responseDto = new MultiResponseDto<>(transactionPage.getContent(), transactionPage);

        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    /**
     * 특정 기간의 환전 거래 내역을 조회.
     *
     * @param startDate       조회 시작일
     * @param endDate         조회 종료일
     * @param status          거래 상태
     * @param authentication 인증 정보
     * @return 환전 거래 내역 목록
     */
    @GetMapping("/managers/et")
    public ResponseEntity getET(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                @RequestParam TransactionEnum status,
                                Authentication authentication) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999999999);

        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        List<ExchangeTransaction> transactionList = exchangeTransactionService.findByStatusAndProcessedAtBetween(status, startDateTime, endDateTime, principal);
        return new ResponseEntity<>(new MultiResponseDto<>(exchangeTransactionAdminResponseMapper.toDto(transactionList)), HttpStatus.OK);
    }

    /**
     * 생성된 환전 거래를 조회.
     *
     * @param startDate       조회 시작일
     * @param endDate         조회 종료일
     * @param status          거래 상태
     * @param authentication  인증 정보
     * @return 생성된 환전 거래 목록과 HTTP 상태코드
     */
    @GetMapping("/managers/et/created")
    public ResponseEntity<List<ExchangeTransactionResponseDTO>> getCreatedET(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam Optional<TransactionEnum> status,
            Authentication authentication) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999999999);
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();

        List<ExchangeTransactionResponseDTO> exchangeTransactionResponseDTOList = exchangeTransactionService.findAllTransactionsByCreatedAtBetweenDatesWithStatusDTO(
                startDateTime, endDateTime, status, principal);

        return ResponseEntity.ok(exchangeTransactionResponseDTOList);
    }

    /**
     * 환전 정산 내역을 조회.
     *
     * @param startDate       조회 시작일
     * @param endDate         조회 종료일
     * @param authentication  인증 정보
     * @return 환전 정산 내역과 HTTP 상태코드
     */
    @GetMapping("/managers/es")
    public ResponseEntity getEs(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        List<ExchangeSettlementAdminDTO> dtoList = exchangeTransactionService.exchangedSettlement(startDate, endDate, principal);
        return new ResponseEntity<>(new MultiResponseDto<>(dtoList), HttpStatus.OK);
    }

    /**
     * 승인된 환전 거래를 조회.
     *
     * @param userId          사용자 ID
     * @param startDate       조회 시작일
     * @param endDate         조회 종료일
     * @param page            페이지 번호
     * @param size            페이지 크기
     * @param authentication  인증 정보
     * @return 승인된 환전 거래 페이지와 HTTP 상태코드
     */
    @GetMapping("/managers/ex-approved")
    public ResponseEntity<Page<ExchangeTransactionApprovedDTO>> getApprovedTransactions(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime;

        if (endDate.equals(LocalDate.now())) {
            endDateTime = LocalDateTime.now();
        } else {
            endDateTime = endDate.plusDays(1).atStartOfDay();
        }

        Page<ExchangeTransactionApprovedDTO> pagedTransactions = exchangeTransactionService.getApprovedTransactionsWithPagination(
                userId, startDateTime, endDateTime, page, size, principal);
        return ResponseEntity.ok(pagedTransactions);
    }

    /**
     * 기간별 환전 거래의 총 충전금액과 평균 충전금액을 조회.
     *
     * @param userId          사용자 ID
     * @param startDate       조회 시작일
     * @param endDate         조회 종료일
     * @param authentication  인증 정보
     * @return 기간별 환전 거래의 총 충전금액과 평균 충전금액 정보와 HTTP 상태코드
     */
    @GetMapping("/managers/ex-approved/summary")
    public ResponseEntity<ExchangeTransactionsSummaryDTO> getTransactionsSummary(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime;

        if (endDate.equals(LocalDate.now())) {
            endDateTime = LocalDateTime.now();
        } else {
            endDateTime = endDate.plusDays(1).atStartOfDay();
        }

        ExchangeTransactionsSummaryDTO summary = exchangeTransactionService.getTransactionsSummary(userId, startDateTime, endDateTime, principal);
        return ResponseEntity.ok(summary);
    }
}
