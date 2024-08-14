package GInternational.server.api.controller;

import GInternational.server.api.dto.RollingTransactionResDTO;
import GInternational.server.api.entity.RollingTransaction;
import GInternational.server.api.service.RollingTransactionService;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v2/managers/rolling/transaction")
@RequiredArgsConstructor
public class RollingTransactionController {

    private final RollingTransactionService rollingTransactionService;

    /**
     * 주어진 날짜 범위에 따라 롤링 거래 정보 조회.
     *
     *
     * @param startDate       조회 시작 날짜
     * @param endDate         조회 종료 날짜
     * @param authentication  사용자 인증 정보, 현재 로그인된 사용자를 식별하기 위해 사용
     * @return                조회된 롤링 거래 정보의 DTO 리스트를 담은 응답 엔터티
     */
    @GetMapping
    public ResponseEntity<List<RollingTransactionResDTO>> getTransactions(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                          Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();

        List<RollingTransaction> transactions = rollingTransactionService.findTransactions(startDate, endDate, principal);
        List<RollingTransactionResDTO> dtos = rollingTransactionService.toDTOList(transactions);

        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }
}


