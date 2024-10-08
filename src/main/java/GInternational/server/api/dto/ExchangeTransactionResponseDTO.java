package GInternational.server.api.dto;

import GInternational.server.api.vo.TransactionEnum;
import GInternational.server.api.vo.TransactionGubunEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ExchangeTransactionResponseDTO {

    private Long id;
    private int lv;
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private TransactionGubunEnum gubun;
    private long exchangeAmount;
    private int bonus;
    private long remainingSportsBalance; // 환전 승인 처리 후 스포츠머니

    private TransactionEnum status;
    private String ip;

    private String site;

    private WalletDetailDTO wallet;

    private String distributor;
    private String store;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd 'T' HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd 'T' HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime processedAt;
}
