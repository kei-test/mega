package GInternational.server.api.service;

import GInternational.server.api.dto.BetLogResponseDTO;
import GInternational.server.api.entity.User;
import GInternational.server.api.repository.UserRepository;
import GInternational.server.common.exception.ExceptionCode;
import GInternational.server.common.exception.RestControllerException;
import GInternational.server.kplay.debit.entity.Debit;
import GInternational.server.kplay.debit.repository.DebitRepository;
import GInternational.server.kplay.game.entity.Game;
import GInternational.server.kplay.game.repository.GameRepository;
import GInternational.server.kplay.product.entity.Product;
import GInternational.server.kplay.product.repository.ProductRepository;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(value = "clientServerTransactionManager")
@RequiredArgsConstructor
public class BetLogService {

    private final DebitRepository debitRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final ProductRepository productRepository;

    /**
     * 주어진 사용자 정보를 바탕으로 모든 베팅 로그를 조회하고 결과를 반환.
     *
     * @param principalDetails 인증된 사용자의 상세 정보
     * @return List<BetLogResponseDTO> 베팅 로그 데이터 목록
     */
    public Page<BetLogResponseDTO> getAllBetLogs(PrincipalDetails principalDetails, LocalDateTime startOfDay, LocalDateTime endDateTime, String resultFilter, Pageable pageable) {
        Page<Debit> pageDebits = debitRepository.findByCreatedAtBetween(startOfDay, endDateTime, pageable);

        List<BetLogResponseDTO> betLogs = pageDebits.getContent().stream().map(debit -> {
            User user = userRepository.findByAasId(debit.getUser_id()).orElseThrow(
                    () -> new RestControllerException(ExceptionCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다.")
            );
            List<Game> games = gameRepository.findByPrdIdAndGameIndex(debit.getPrd_id(), debit.getGame_id());
            String gameName = games.isEmpty() ? "Unknown Game" : games.get(0).getName();
            Optional<Product> productOpt = productRepository.findByPrdId(debit.getPrd_id());
            String prdName = productOpt.map(Product::getPrd_name).orElse("Unknown Product");

            String gameType = determineGameType(debit.getPrd_id());
            BigDecimal betAmount = calculateBetAmount(debit);
            BigDecimal winAmount = calculateWinAmount(debit);
            String gameResult = determineResult(betAmount, winAmount);

            return new BetLogResponseDTO(
                    user.getUsername(),
                    user.getNickname(),
                    gameName,
                    gameType,
                    debit.getCreatedAt(),
                    betAmount,
                    winAmount,
                    gameResult,
                    prdName
            );
        }).collect(Collectors.toList());

        return new PageImpl<>(betLogs, pageable, pageDebits.getTotalElements());
    }


    /**
     * 제품 ID를 기반으로 게임 유형을 결정.
     *
     * @param prdId 제품 ID
     * @return String 게임 유형을 나타내는 문자열 (라이브 카지노, 슬롯, 기타)
     */
    private String determineGameType(int prdId) {
        if (prdId >= 1 && prdId <= 100) return "라이브 카지노";
        else if (prdId >= 101 && prdId <= 300) return "슬롯";
        else return "기타";
    }

    /**
     * 베팅에 사용된 총 금액을 계산.
     *
     * @param debit 베팅 정보를 담고 있는 Debit 엔티티
     * @return BigDecimal 베팅 금액
     */
    private BigDecimal calculateBetAmount(Debit debit) {
        return BigDecimal.valueOf(debit.getAmount() + debit.getCredit_amount());
    }

    /**
     * 베팅에서 승리한 금액을 계산.
     *
     * @param debit 베팅 정보를 담고 있는 Debit 엔티티
     * @return BigDecimal 승리 금액, Credit 정보가 없을 경우 0
     */
    private BigDecimal calculateWinAmount(Debit debit) {
        return debit.getCredit() != null ? BigDecimal.valueOf(debit.getCredit().getAmount()) : BigDecimal.ZERO;
    }

    /**
     * 베팅 결과를 결정.
     *
     * @param betAmount 베팅 금액
     * @param winAmount 승리 금액
     * @return String 베팅 결과 ("당첨" 또는 "낙첨")
     */
    private String determineResult(BigDecimal betAmount, BigDecimal winAmount) {
        return winAmount.compareTo(betAmount) > 0 ? "당첨" : "낙첨";
    }
}
