package GInternational.server.api.controller;

import GInternational.server.api.dto.RechargeTransactionResDTO;
import GInternational.server.common.dto.MultiResponseDto;
import GInternational.server.api.dto.RechargeTransactionApprovedDTO;
import GInternational.server.api.dto.RechargeTransactionsSummaryDTO;
import GInternational.server.api.entity.RechargeTransaction;
import GInternational.server.api.mapper.RechargeTransactionAdminResponseMapper;
import GInternational.server.api.mapper.RechargeSettlementAdminMapper;
import GInternational.server.api.mapper.RechargeTransactionResponseMapper;
import GInternational.server.api.service.RechargeTransactionService;
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
public class RechargeTransactionController {


    private final RechargeTransactionService rechargeTransactionService;
    private final RechargeTransactionResponseMapper mapper;
    private final RechargeTransactionAdminResponseMapper rechargeTransactionAdminResponseMapper;
    private final RechargeSettlementAdminMapper rechargeSettlementAdminMapper;

    /**
     * 사용자 ID로 충전 거래 내역 조회.
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호
     * @param size 페이지 당 항목 수
     * @param authentication 인증 정보
     * @return 조회된 충전 거래 내역의 페이지
     */
    @GetMapping("/users/{userId}/recharge/transaction")
    public ResponseEntity getTransaction(@PathVariable("userId") @Positive Long userId,
                                         @RequestParam int page,
                                         @RequestParam int size,
                                         Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        Page<RechargeTransaction> transactions = rechargeTransactionService.getTransactionsByUserId(userId,page,size,principal);
        List<RechargeTransaction> list = transactions.getContent();
        return new ResponseEntity<>(new MultiResponseDto<>(mapper.toDto(list), transactions), HttpStatus.OK);
    }

    /**
     * 지정된 기간 동안의 충전 거래 조회. 상태에 따른 필터링 적용.
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param status 거래 상태
     * @param authentication 인증 정보
     * @return 조회된 충전 거래 목록
     */
    @GetMapping("/managers/rt")
    public ResponseEntity getRT(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                @RequestParam TransactionEnum status,
                                Authentication authentication) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999999999);

        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        List<RechargeTransaction> rechargeTransactionList = rechargeTransactionService.findAllByProcessedAtBetweenAndStatus(startDateTime, endDateTime, status, principal);
        return new ResponseEntity<>(new MultiResponseDto<>(rechargeTransactionAdminResponseMapper.toDto(rechargeTransactionList)), HttpStatus.OK);
    }

    /**
     * 생성된 충전 거래 조회. 지정된 기간과 상태에 따라 결과 필터링.
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param status 거래 상태
     * @param authentication 인증 정보
     * @return 조회된 충전 거래 목록
     */
    @GetMapping("/managers/rt/created")
    public ResponseEntity<List<RechargeTransactionResDTO>> getCreatedRT(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam Optional<TransactionEnum> status,
            Authentication authentication) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999999999);
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();

        List<RechargeTransactionResDTO> rechargeTransactionResDTOList = rechargeTransactionService.findAllTransactionsByCreatedAtBetweenDatesWithStatusDTO(
                startDateTime, endDateTime, status, principal);

        return ResponseEntity.ok(rechargeTransactionResDTOList);
    }

    /**
     * 충전 정산 내역 조회. 선택적으로 시작 및 종료 날짜를 지정할 수 있음.
     *
     * @param startDate 시작 날짜 (선택 사항)
     * @param endDate 종료 날짜 (선택 사항)
     * @param authentication 인증 정보
     * @return 조회된 충전 정산 내역
     */
    @GetMapping("/managers/rs")
    public ResponseEntity getrs(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        List<RechargeTransaction> rechargeTransactions = rechargeTransactionService.rechargedSettlement(startDate, endDate, principal);
        return new ResponseEntity<>(rechargeSettlementAdminMapper.toDto(rechargeTransactions), HttpStatus.OK);
    }

    /**
     * 승인된 거래 내역 조회.
     *
     * @param userId 사용자 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param page 페이지 번호
     * @param size 페이지 당 항목 수
     * @param authentication 인증 정보
     * @return 페이지화된 승인된 거래 내역
     */
    @GetMapping("/managers/approved")
    public ResponseEntity<Page<RechargeTransactionApprovedDTO>> getApprovedTransactions(@RequestParam Long userId,
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

        Page<RechargeTransactionApprovedDTO> pagedTransactions = rechargeTransactionService.getApprovedTransactionsWithPagination(
                userId, startDateTime, endDateTime, page, size, principal);
        return ResponseEntity.ok(pagedTransactions);
    }

    /**
     * 지정된 사용자와 기간 동안의 거래 요약 정보 조회.
     *
     * @param userId 사용자 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param authentication 인증 정보
     * @return 조회된 거래 요약 정보
     */
    @GetMapping("/managers/approved/summary")
    public ResponseEntity<RechargeTransactionsSummaryDTO> getTransactionsSummary(@RequestParam Long userId,
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

        RechargeTransactionsSummaryDTO summary = rechargeTransactionService.getTransactionsSummary(userId, startDateTime, endDateTime, principal);
        return ResponseEntity.ok(summary);
    }
}
