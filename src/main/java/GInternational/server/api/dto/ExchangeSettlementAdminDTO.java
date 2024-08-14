package GInternational.server.api.dto;

import GInternational.server.api.entity.ExchangeTransaction;
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
public class ExchangeSettlementAdminDTO {
    private Long id;
    private int lv;
    private String username;
    private String nickname;
    private String ownerName;
    private String bankName;
    private String number;
    private String phone;
    private String distributor; //총판
    private long exchangeAmount;
    private long exchangedCount;
    private String ip;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd 'T' HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd 'T' HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime processedAt;
    private TransactionEnum status;
    private TransactionGubunEnum gubun;

}
