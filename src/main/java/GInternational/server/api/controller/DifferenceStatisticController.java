package GInternational.server.api.controller;

import GInternational.server.api.dto.DifferenceStatisticAccountRequestDTO;
import GInternational.server.api.dto.DifferenceStatisticRequestDTO;
import GInternational.server.api.entity.DifferenceStatistic;
import GInternational.server.api.service.DifferenceStatisticService;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v2/managers")
@RequiredArgsConstructor
public class DifferenceStatisticController {

    private final DifferenceStatisticService differenceStatisticService;

    // 임시 계좌 정보를 세션에 저장
    @PostMapping("/difference-statistic/insert/account")
    public ResponseEntity<?> addTempSavedAccount(@RequestBody DifferenceStatisticAccountRequestDTO accountRequestDTO,
                                                 Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        differenceStatisticService.addTempSavedAccount(accountRequestDTO, principal);
        return ResponseEntity.ok().build();
    }

    // 세션에서 임시 저장된 계좌 정보를 조회
    @GetMapping("/difference-statistic/all")
    public ResponseEntity<List<DifferenceStatisticAccountRequestDTO>> getTempSavedAccounts(HttpServletRequest request,
                                                                                           Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        List<DifferenceStatisticAccountRequestDTO> tempAccounts = differenceStatisticService.getTempSavedAccounts(principal);
        return ResponseEntity.ok(tempAccounts);
    }

    // 세션에서 임시 저장된 계좌 정보 업데이트
    @PostMapping("/difference-statistic/update/account")
    public ResponseEntity<?> updateTempSavedAccount(@RequestBody DifferenceStatisticAccountRequestDTO updatedAccount,
                                                    Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        differenceStatisticService.updateAccount(updatedAccount, principal);
        return ResponseEntity.ok().build();
    }

    // 세션에서 임시 저장된 계좌 정보 삭제
    @DeleteMapping("/difference-statistic/delete/account/{accountId}")
    public ResponseEntity<?> deleteTempSavedAccount(@PathVariable Long accountId,
                                                    Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        differenceStatisticService.deleteAccount(accountId, principal);
        return ResponseEntity.ok().build();
    }

    // 세션에서 임시 저장된 계좌 정보를 데이터베이스에 저장
    @PostMapping("/difference-statistic/save")
    public ResponseEntity<?> saveAccountsFromSessionToDatabase(@RequestBody DifferenceStatisticRequestDTO requestDTO,
                                                               Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        differenceStatisticService.saveAccountsFromSessionToDatabase(requestDTO, principal);
        return ResponseEntity.ok().build();
    }

    // 날짜 기준으로 DifferenceStatistic 목록 조회
    @GetMapping("/difference-statistic/list")
    public ResponseEntity<List<DifferenceStatistic>> getDifferenceStatisticsByDate(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                                                   Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        List<DifferenceStatistic> statistics = differenceStatisticService.findDifferenceStatisticsByDate(date, principal);
        return ResponseEntity.ok(statistics);
    }

    // DifferenceStatistic 엔터티를 id 기준으로 삭제
    @DeleteMapping("/difference-statistic/delete/{id}")
    public ResponseEntity<?> deleteDifferenceStatisticById(@PathVariable Long id,
                                                           Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        differenceStatisticService.deleteDifferenceStatisticById(id, principal);
        return ResponseEntity.ok().build();
    }
}
