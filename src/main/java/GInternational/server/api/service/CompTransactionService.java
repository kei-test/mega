package GInternational.server.api.service;

import GInternational.server.api.dto.CompTransactionResDTO;
import GInternational.server.api.entity.CompTransaction;
import GInternational.server.api.repository.CompTransactionRepository;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(value = "clientServerTransactionManager")
@RequiredArgsConstructor
public class CompTransactionService {

    private final CompTransactionRepository compTransactionRepository;

    /**
     * 주어진 날짜 범위에 따라 롤링 거래를 검색하여 리스트로 반환.
     *
     *
     * @param startDate         조회 시작 날짜
     * @param endDate           조회 종료 날짜
     * @param principalDetails  현재 사용자의 인증 정보, 사용자의 권한 및 식별 정보를 포함
     * @return                  조회된 롤링 거래 정보의 리스트
     */
    public List<CompTransaction> findTransactions(LocalDate startDate, LocalDate endDate, PrincipalDetails principalDetails) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        return compTransactionRepository.findByCreatedAtBetween(startDateTime, endDateTime);
    }

    /**
     * 롤링 거래 엔티티 목록을 DTO 목록으로 변환.
     *
     * @param transactions   롤링 거래 엔티티 목록
     * @return               DTO로 변환된 롤링 거래 목록
     */
    public List<CompTransactionResDTO> toDTOList(List<CompTransaction> transactions) {
        List<CompTransactionResDTO> dtoList = new ArrayList<>();
        for (CompTransaction transaction : transactions) {
            CompTransactionResDTO dto = new CompTransactionResDTO();
            dto.setId(transaction.getId());
            dto.setUserId(transaction.getUserId());
            dto.setLv(transaction.getLv());
            dto.setUsername(transaction.getUsername());
            dto.setNickname(transaction.getNickname());
            dto.setCreatedAt(transaction.getCreatedAt());
            dto.setProcessedAt(transaction.getProcessedAt());
            dto.setLastDayChargeBalance(transaction.getLastDayChargeSportsBalance());
            dto.setCalculatedReward(transaction.getCalculatedReward());
            dto.setRate(transaction.getRate());
            dto.setLastDayAmount(transaction.getLastDayAmount());
            dto.setSportsBalance(transaction.getSportsBalance());
            dto.setCasinoBalance(transaction.getCasinoBalance());
            dto.setStatus(transaction.getStatus());
            dto.setUserIp(transaction.getUserIp());
            dtoList.add(dto);
        }
        return dtoList;
    }
}
