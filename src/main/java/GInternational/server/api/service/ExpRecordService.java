package GInternational.server.api.service;

import GInternational.server.api.dto.ExpRecordResponseDTO;
import GInternational.server.api.entity.ExpRecord;
import GInternational.server.api.entity.ExpSetting;
import GInternational.server.api.entity.User;
import GInternational.server.api.entity.Wallet;
import GInternational.server.api.repository.ExpRecordRepository;
import GInternational.server.api.repository.ExpSettingRepository;
import GInternational.server.api.repository.UserRepository;
import GInternational.server.api.repository.WalletRepository;
import GInternational.server.api.vo.ExpRecordEnum;
import GInternational.server.common.exception.ExceptionCode;
import GInternational.server.common.exception.RestControllerException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Service
@Transactional(value = "clientServerTransactionManager")
@RequiredArgsConstructor
public class ExpRecordService {

    private final ExpRecordRepository expRecordRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final ExpSettingRepository expSettingRepository;

    public void recordDailyExp(Long userId, String username, String nickname, long exp, String ip, ExpRecordEnum content) {
        User user = userRepository.findById(userId).orElseThrow
                (()-> new RestControllerException(ExceptionCode.USER_NOT_FOUND, "유저 정보 없음"));
        Wallet wallet = walletRepository.findById(userId).orElse(null);

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        // 오늘 해당 userId로 기록된 경험치가 있는지 확인
        if (expRecordRepository.existsByUserIdAndContentAndDateRange(userId, content, startOfDay, endOfDay)) {
            return;
        }

        // 기록이 없다면 새로운 경험치 기록 추가
        ExpRecord expRecord = new ExpRecord();
        expRecord.setUserId(userId);
        expRecord.setUsername(username);
        expRecord.setNickname(nickname);
        expRecord.setExp(exp);
        expRecord.setIp(ip);
        expRecord.setContent(content);
        expRecord.setCreatedAt(LocalDateTime.now());
        expRecordRepository.save(expRecord);

        // 누적 베팅 금액에 따른 추가 경험치 로직
        long additionalExp = 0;
        // 지갑 정보가 있을 경우에만 누적 베팅 금액에 따른 추가 경험치 로직 실행
        if (wallet != null) {
            switch (content) {
                case 스포츠베팅경험치:
                    additionalExp = calculateAdditionalExp(wallet.getAccumulatedSportsBet(), exp);
                    break;
                case 카지노베팅경험치:
                    additionalExp = calculateAdditionalExp(wallet.getAccumulatedCasinoBet(), exp);
                    break;
                case 슬롯베팅경험치:
                    additionalExp = calculateAdditionalExp(wallet.getAccumulatedSlotBet(), exp);
                    break;
            }
        }

        // 누적 베팅 경험치 추가 기록
        if (additionalExp > 0) {
            ExpRecord additionalExpRecord = new ExpRecord();
            additionalExpRecord.setUserId(userId);
            additionalExpRecord.setUsername(username);
            additionalExpRecord.setNickname(nickname);
            additionalExpRecord.setExp(additionalExp);
            additionalExpRecord.setIp(ip);
            additionalExpRecord.setContent(determineAccumulatedContent(content, wallet)); // 누적 경험치 타입 결정
            additionalExpRecord.setCreatedAt(LocalDateTime.now());
            expRecordRepository.save(additionalExpRecord);
        }

        Optional<ExpSetting> expSettingOpt = expSettingRepository.findByLv(user.getLv() + 1);
        long nextLevelExpRequired = expSettingOpt.map(expSetting -> expSetting.getMinExp() - user.getExp()).orElse(0L);

        long currentExp = user.getExp();
        user.setExp(currentExp + exp + additionalExp);
        user.setNextLevelExp(nextLevelExpRequired);
        userRepository.save(user);
    }

    public void recordDailyExpUpToFiveTime(Long userId, String username, String nickname, long exp, String ip, ExpRecordEnum content) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new RestControllerException(ExceptionCode.USER_NOT_FOUND, "유저 정보 없음"));

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        // 오늘 해당 userId와 ExpRecordEnum 값으로 기록된 경험치 기록의 수를 확인
        long countTodayExpRecordsForContent = expRecordRepository.countByUserIdAndContentAndDateRange(userId, content, startOfDay, endOfDay);

        Optional<ExpSetting> expSettingOpt = expSettingRepository.findByLv(user.getLv() + 1);
        long nextLevelExpRequired = expSettingOpt.map(expSetting -> expSetting.getMinExp() - user.getExp()).orElse(0L);

        // 하루에 해당 ExpRecordEnum 값으로 최대 5개까지의 경험치 기록이 가능
        if (countTodayExpRecordsForContent < 5) {
            // 기록이 5개 미만인 경우 새로운 경험치 기록 추가
            ExpRecord expRecord = new ExpRecord();
            expRecord.setUserId(userId);
            expRecord.setUsername(username);
            expRecord.setNickname(nickname);
            expRecord.setExp(exp);
            expRecord.setIp(ip);
            expRecord.setContent(content);
            expRecord.setCreatedAt(LocalDateTime.now());
            expRecordRepository.save(expRecord);

            // 사용자의 현재 경험치를 업데이트
            long currentExp = user.getExp();
            user.setExp(currentExp + exp);
            user.setNextLevelExp(nextLevelExpRequired);
            userRepository.save(user);
        }
    }

    public Page<ExpRecordResponseDTO> findExpRecords(String username, String nickname, ExpRecordEnum content, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        Specification<ExpRecord> spec = Specification.where(null);

        if (username != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("username"), username));
        }

        if (nickname != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("nickname"), nickname));
        }

        if (content != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("content"), content));
        }

        Page<ExpRecord> records = expRecordRepository.findAll(spec, pageable);
        return records.map(this::convertToDto);
    }

    private long calculateAdditionalExp(long accumulatedBet, long currentBet) {
        long newTotal = accumulatedBet + currentBet;
        long oldMilestones = accumulatedBet / 1_000_000;
        long newMilestones = newTotal / 1_000_000;
        return (newMilestones - oldMilestones) * 10;
    }

    private ExpRecordEnum determineAccumulatedContent(ExpRecordEnum content, Wallet wallet) {
        // 이 메서드는 content에 따라 적절한 누적 경험치 타입을 반환합니다.
        // 예: 스포츠베팅경험치 -> 스포츠베팅누적경험치
        switch (content) {
            case 스포츠베팅경험치:
                return ExpRecordEnum.스포츠베팅누적경험치;
            case 카지노베팅경험치:
                return ExpRecordEnum.카지노베팅누적경험치;
            case 슬롯베팅경험치:
                return ExpRecordEnum.슬롯베팅누적경험치;
            default:
                // 이 경우는 발생하지 않아야 하며, 발생한다면 적절한 예외 처리가 필요합니다.
                throw new IllegalArgumentException("Unsupported content type for accumulation");
        }
    }

    private ExpRecordResponseDTO convertToDto(ExpRecord record) {
        return new ExpRecordResponseDTO(
                record.getId(),
                record.getUserId(),
                record.getUsername(),
                record.getNickname(),
                record.getExp(),
                record.getIp(),
                record.getContent()
        );
    }
}
