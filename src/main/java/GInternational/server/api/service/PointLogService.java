package GInternational.server.api.service;

import GInternational.server.api.vo.PointLogCategoryEnum;
import GInternational.server.common.exception.ExceptionCode;
import GInternational.server.common.exception.RestControllerException;
import GInternational.server.api.dto.PointLogResponseDTO;
import GInternational.server.api.entity.PointLog;
import GInternational.server.api.mapper.PointLogResponseMapper;
import GInternational.server.api.repository.PointLogRepository;
import GInternational.server.security.auth.PrincipalDetails;
import GInternational.server.api.entity.User;
import GInternational.server.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(value = "clientServerTransactionManager")
@RequiredArgsConstructor
public class PointLogService {

    private final PointLogRepository pointLogRepository;
    private final PointLogResponseMapper pointLogResponseMapper;
    private final UserRepository userRepository;

    /**
     * 모든 포인트 적립 내역을 조회하고, 필요한 경우 카테고리별로 정렬.
     *
     * @param category 정렬을 위한 카테고리 (옵션)
     * @return 정렬된 포인트 적립 내역 목록
     */
    public List<PointLogResponseDTO> getAllPointTrackingTransactions(PrincipalDetails principalDetails, Long userId, PointLogCategoryEnum category, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.MIN;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.MAX;

        List<PointLog> transactions;

        if (userId != null) {
            User user = userRepository.findById(userId).orElseThrow(() -> new RestControllerException(ExceptionCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));
            if (category != null && startDate != null && endDate != null) {
                transactions = pointLogRepository.findByUserIdAndCategoryAndCreatedAtBetweenOrderByCreatedAtDesc(user, category, startDateTime, endDateTime);
            } else if (category != null) {
                transactions = pointLogRepository.findByUserIdAndCategoryAndCreatedAtBetweenOrderByCreatedAtDesc(user, category, startDateTime, endDateTime);
            } else if (startDate != null && endDate != null) {
                transactions = pointLogRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(user, startDateTime, endDateTime);
            } else {
                transactions = pointLogRepository.findByUserIdOrderByCreatedAtDesc(user);
            }
        } else {
            if (category != null && startDate != null && endDate != null) {
                transactions = pointLogRepository.findByCategoryAndCreatedAtBetweenOrderByCreatedAtDesc(category, startDateTime, endDateTime);
            } else if (category != null) {
                transactions = pointLogRepository.findByCategoryOrderByCreatedAtDesc(category);
            } else if (startDate != null && endDate != null) {
                transactions = pointLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDateTime, endDateTime);
            } else {
                transactions = pointLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            }
        }

        return transactions.stream()
                .map(pointLogResponseMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 포인트 적립 또는 사용 내역을 PointLog에 기록.
     *
     * @param userId     사용자 ID
     * @param points     적립 또는 사용된 포인트 수
     * @param category   포인트 내역의 카테고리
     * @param userIp     사용자 IP
     */
    public void recordPointLog(Long userId, Long points, PointLogCategoryEnum category, String userIp, String memo) {
        User user = userRepository.findById(userId).orElseThrow
                (() -> new RestControllerException(ExceptionCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        PointLog pointLog = new PointLog();
        pointLog.setUserId(user);
        pointLog.setUsername(user.getUsername());
        pointLog.setNickname(user.getNickname());
        pointLog.setPoint((long) points.intValue());
        pointLog.setFinalPoint(user.getWallet().getPoint());
        pointLog.setCreatedAt(LocalDateTime.now());
        pointLog.setCategory(category.getValue());
        pointLog.setIp(userIp);
        pointLog.setMemo(memo);

        pointLogRepository.save(pointLog);
    }
}