package GInternational.server.kplay.bonus.service;

import GInternational.server.common.exception.ExceptionCode;
import GInternational.server.common.exception.RestControllerException;
import GInternational.server.api.entity.Wallet;
import GInternational.server.api.repository.WalletRepository;
import GInternational.server.kplay.bonus.dto.BonusRequestDTO;
import GInternational.server.kplay.bonus.dto.BonusResponseDTO;
import GInternational.server.kplay.bonus.repository.BonusRepository;
import GInternational.server.api.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(value = "clientServerTransactionManager")
public class BonusService {

    private final WalletRepository walletRepository;
    private final BonusRepository bonusRepository;

    @Value("${secret.secret-key}")
    private String secretKey;

    /**
     * 사용자에게 보너스를 지급. 사용자 인증과 보너스 금액 지급을 처리.
     *
     * @param bonusRequestDTO 보너스 지급 요청 정보를 담은 DTO
     * @param secretHeader 요청 헤더에서 전달된 비밀 키
     * @return BonusResponseDTO 보너스 지급 응답 DTO
     */
    public BonusResponseDTO calledBonus(BonusRequestDTO bonusRequestDTO,String secretHeader) {
        User user = bonusRepository.findByAasId(bonusRequestDTO.getUser_id()).orElse(null);

        if (user == null) {
            return BonusResponseDTO.createFailureResponse("INVALID_USER");
        } else if (!secretHeader.equals(secretKey)) {
            return BonusResponseDTO.createFailureResponse("ACCESS_DENIED");
        }

        Wallet wallet = walletRepository.findById(user.getWallet().getId()).orElseThrow
                (() -> new RestControllerException(ExceptionCode.WALLET_INFO_NOT_FOUND, "지갑 정보 없음"));
        wallet.setCasinoBalance(user.getWallet().getCasinoBalance() + bonusRequestDTO.getAmount());
        walletRepository.save(wallet);

        int status = (secretHeader.equals(secretKey) &&
                user.getAasId().equals(bonusRequestDTO.getUser_id())) ? 1 : 0;

        String error = getErrorMessage(status, user, secretHeader);

        if (status == 1) {
            return new BonusResponseDTO(status, wallet.getCasinoBalance());
        } else {
            return BonusResponseDTO.createFailureResponse(error);
        }
    }

    /**
     * 보너스 지급 실패 시 오류 메시지를 결정.
     *
     * @param status 상태 코드 (성공: 1, 실패: 0)
     * @param user 조회 요청한 유저
     * @param secretHeader 요청 헤더에서 전달된 비밀 키
     * @return String 오류 메시지
     */
    public String getErrorMessage(int status, User user, String secretHeader) {
        if (status == 0) {
            if (user.getAasId() == null) {
                return "INVALID_USER";
            } else if (!secretHeader.equals(secretKey)) {
                return "ACCESS_DENIED";
            } else {
                return "UNKNOWN_ERROR";
            }
        }
        return null;
    }
}


