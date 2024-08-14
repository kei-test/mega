package GInternational.server.api.controller;

import GInternational.server.security.auth.PrincipalDetails;
import GInternational.server.api.dto.LoginHistoryDTO;
import GInternational.server.api.service.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/managers/history")
public class LoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    /**
     * 특정 날짜 범위에 대한 로그인 이력 조회.
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param authentication 인증 정보
     * @return 해당 날짜 범위의 로그인 이력 목록
     */
    @GetMapping("/range")
    public List<LoginHistoryDTO> getLoginHistoryByDateRange(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                                            Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        return loginHistoryService.getLoginHistoryByDateRange(startDate, endDate, principal);
    }


}
