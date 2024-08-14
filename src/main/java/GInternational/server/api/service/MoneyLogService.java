package GInternational.server.api.service;

import GInternational.server.common.exception.ExceptionCode;
import GInternational.server.common.exception.RestControllerException;
import GInternational.server.api.dto.MoneyLogResponseDTO;
import GInternational.server.api.entity.MoneyLog;
import GInternational.server.api.mapper.MoneyLogResponseMapper;
import GInternational.server.api.repository.MoneyLogRepository;
import GInternational.server.api.vo.MoneyLogCategoryEnum;
import GInternational.server.security.auth.PrincipalDetails;
import GInternational.server.api.entity.User;
import GInternational.server.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(value = "clientServerTransactionManager")
@RequiredArgsConstructor
public class MoneyLogService {

    private final MoneyLogRepository moneyLogRepository;
    private final MoneyLogResponseMapper moneyLogResponseMapper;
    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * 모든 머니 적립 내역을 조회하고, 필요한 경우 카테고리별로 정렬.
     *
     * @param category 정렬을 위한 카테고리 (옵션)
     * @return 정렬된 머니 적립 내역 목록
     */
    public List<MoneyLogResponseDTO> getAllMoneyTrackingTransactions(PrincipalDetails principalDetails, Long userId, MoneyLogCategoryEnum category, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.MIN;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.MAX;

        List<MoneyLog> transactions;
        if (userId != null) {
            if (category != null) {
                transactions = moneyLogRepository.findByUserIdAndCreatedAtBetweenAndCategory(userId, startDateTime, endDateTime, category);
            } else {
                transactions = moneyLogRepository.findByUserIdAndCreatedAtBetween(userId, startDateTime, endDateTime);
            }
        } else {
            if (category != null) {
                transactions = moneyLogRepository.findByCreatedAtBetweenAndCategory(startDateTime, endDateTime, category);
            } else {
                transactions = moneyLogRepository.findByCreatedAtBetween(startDateTime, endDateTime);
            }
        }

        // 데이터가 없는 경우 빈 리스트 반환
        if (transactions.isEmpty()) {
            return Collections.emptyList();
        }

        return transactions.stream()
                .map(moneyLogResponseMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 머니 사용 기록을 추가합니다.
     *
     * @param userId             사용자 ID.
     * @param usedSportsBalance  사용된 금액.
     * @param finalSportsBalance 최종 금액.
     * @param category           카테고리.
     * @param bigo               비고.
     */
    public void recordMoneyUsage(Long userId, Long usedSportsBalance, Long finalSportsBalance, MoneyLogCategoryEnum category, String bigo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RestControllerException(ExceptionCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));
        MoneyLog moneyLog = new MoneyLog();
        moneyLog.setUser(user);
        moneyLog.setUsername(user.getUsername());
        moneyLog.setNickname(user.getNickname());
        moneyLog.setUsedSportsBalance(usedSportsBalance);
        moneyLog.setFinalSportsBalance(finalSportsBalance);
        moneyLog.setCategory(category);
        moneyLog.setBigo(bigo);
        moneyLog.setSite("mega");
        moneyLogRepository.save(moneyLog);
    }
}