package GInternational.server.api.controller;

import GInternational.server.api.dto.BetLogResponseDTO;
import GInternational.server.api.service.BetLogService;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v2/managers/bet-log")
public class BetLogController {

    private final BetLogService betLogService;

    /**
     * 지정된 날짜 범위와 인증된 사용자를 기반으로 모든 베팅 로그를 조회.
     *
     * @param authentication 스프링 시큐리티의 Authentication 객체, 현재 인증된 사용자 정보를 포함.
     * @param startOfDay 조회할 베팅 로그의 시작 날짜 (년-월-일 형식, 예: "2023-03-15")
     * @param endOfDay 조회할 베팅 로그의 종료 날짜 (년-월-일 형식, 예: "2023-03-15")
     * @return ResponseEntity<List<BetLogResponseDTO>> 조회된 베팅 로그 목록을 담고 있는 ResponseEntity 객체.
     */
    @GetMapping
    public ResponseEntity<Page<BetLogResponseDTO>> getBetLogsByDate(Authentication authentication,
                                                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startOfDay,
                                                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endOfDay,
                                                                    @RequestParam(required = false) String result,
                                                                    @RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        LocalDateTime startDateTime = startOfDay.atStartOfDay();
        LocalDateTime endDateTime = endOfDay.atTime(23, 59, 59);
        Pageable pageable = PageRequest.of(page -1, size);

        Page<BetLogResponseDTO> betLogs = betLogService.getAllBetLogs(principal, startDateTime, endDateTime, result, pageable);
        return ResponseEntity.ok(betLogs);
    }
}
