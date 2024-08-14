package GInternational.server.api.controller;

import GInternational.server.api.entity.LevelUp;
import GInternational.server.api.service.LevelUpService;
import GInternational.server.api.vo.LevelUpTransactionEnum;
import GInternational.server.common.exception.RestControllerException;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class LevelUpController {

    private final LevelUpService levelUpService;

    // 레벨업 신청
    @PostMapping("/users/level-up/apply")
    public ResponseEntity<String> applyLevelUp(Authentication authentication) {
        try {
            PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
            levelUpService.applyLevelUp(principal);
            return new ResponseEntity<>("레벨업 신청이 완료되었습니다.", HttpStatus.CREATED);
        } catch (RestControllerException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // 레벨업 신청 승인
    @PutMapping("/managers/level-up/approve/{levelUpId}")
    public ResponseEntity<String> approveLevelUp(@PathVariable Long levelUpId,
                                                 Authentication authentication) {
        try {
            PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
            levelUpService.approveLevelUp(levelUpId, principal);
            return ResponseEntity.ok("레벨업 신청이 승인되었습니다.");
        } catch (RestControllerException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // 레벨업 신청 취소(거절)
    @PutMapping("/managers/level-up/cancel/{levelUpId}")
    public ResponseEntity<String> cancelLevelUp(@PathVariable Long levelUpId,
                                                Authentication authentication) {
        try {
            PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
            levelUpService.cancelLevelUp(levelUpId, principal);
            return ResponseEntity.ok("레벨업 신청이 취소되었습니다.");
        } catch (RestControllerException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/users/level-up/search")
    public ResponseEntity<List<LevelUp>> searchLevelUps(
            @RequestParam(value = "status", required = false) LevelUpTransactionEnum status,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "memo", required = false) String memo) {
        List<LevelUp> levelUps = levelUpService.searchLevelUps(status, username, nickname, memo);
        return ResponseEntity.ok(levelUps);
    }
}
