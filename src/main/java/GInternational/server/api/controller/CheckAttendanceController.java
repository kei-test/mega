package GInternational.server.api.controller;

import GInternational.server.api.dto.CheckAttendanceMonthlyDTO;
import GInternational.server.api.dto.NewCheckAttendanceDTO;
import GInternational.server.api.service.CheckAttendanceService;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class CheckAttendanceController {

    private final CheckAttendanceService checkAttendanceService;

    /**
     * 사용자의 현재 월 출석 정보 조회.
     * 사용자가 이번 달에 출석체크한 날짜와 총 출석일수를 반환.
     *
     * @param userId         사용자의 고유 식별자
     * @param authentication 현재 인증된 사용자의 상세 정보
     * @return ResponseEntity 현재 월의 사용자 출석 정보를 담은 DTO
     */
    @GetMapping("/managers/attendance/info/{userId}")
    public ResponseEntity<CheckAttendanceMonthlyDTO> getCurrentMonthAttendanceInfo(@PathVariable("userId") @Positive Long userId,
                                                                                   Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        {
            CheckAttendanceMonthlyDTO attendanceInfo = checkAttendanceService.getCurrentMonthAttendanceInfo(userId, principal);
            return new ResponseEntity<>(attendanceInfo, HttpStatus.OK);
        }
    }

    /**
     * 출석 기록 전체 조회.
     *
     * @param username   유저 이름
     * @param nickname   유저 닉네임
     * @param startDate  시작일
     * @param endDate    종료일
     * @param authentication 현재 인증된 사용자의 상세 정보
     * @return ResponseEntity 출석 기록 리스트
     */
    @GetMapping("/managers/attendance")
    public ResponseEntity<List<NewCheckAttendanceDTO>> getAllAttendance(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        List<NewCheckAttendanceDTO> attendanceList = checkAttendanceService.getAllAttendance(username, nickname, startDate, endDate, principal);
        return new ResponseEntity<>(attendanceList, HttpStatus.OK);
    }
}
