package GInternational.server.api.controller;

import GInternational.server.api.dto.PointLogResponseDTO;
import GInternational.server.api.service.PointLogService;
import GInternational.server.api.vo.PointLogCategoryEnum;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v2/users/point-log")
@RequiredArgsConstructor
public class PointLogController {

    private final PointLogService pointLogService;

    /**
     * 모든 포인트 적립 내역을 조회하거나, 필요한 경우 카테고리별로 조회.
     *
     * @param category 카테고리별 정렬을 위한 파라미터 (옵션)
     * @return ResponseEntity 포인트 적립 내역 목록을 담은 응답
     */
    @GetMapping
    public ResponseEntity<List<PointLogResponseDTO>> getAllPointTrackingTransactions(@RequestParam(required = false) Long userId,
                                                                                     @RequestParam(required = false) PointLogCategoryEnum category,
                                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                                     Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        List<PointLogResponseDTO> transactions = pointLogService.getAllPointTrackingTransactions(principal, userId, category, startDate, endDate);
        return ResponseEntity.ok(transactions);
    }
}

