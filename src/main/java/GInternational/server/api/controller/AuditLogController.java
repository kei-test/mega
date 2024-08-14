package GInternational.server.api.controller;

import GInternational.server.api.entity.AuditLog;
import GInternational.server.api.service.AuditLogService;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 왼쪽메뉴 [16] 관리자 관리, 85 활동로그/관리자 활동 로그
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/v2/managers/audit-log")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 모든 활동 로그를 조회.
     *
     * @return ResponseEntity<List<AuditLog>> 모든 활동 로그 목록
     */
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllAuditLogs(Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        List<AuditLog> logs = auditLogService.getAllAuditLogs(principal);
        return ResponseEntity.ok(logs);
    }

    /**
     * 특정 ID의 활동 로그 조회.
     *
     * @param id 활동 로그의 ID
     * @return ResponseEntity<AuditLog> 특정 ID를 가진 활동 로그
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> getAuditLogById(@PathVariable Long id,
                                                    Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        return auditLogService.getAuditLogById(id, principal)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
