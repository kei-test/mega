package GInternational.server.api.dto;

import GInternational.server.api.vo.CouponTransactionEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CouponResponseDTO {

    private Long id;
    private int sportsBalance; // 쿠폰 금액
    private int point; // 포인트 금액
    private LocalDateTime createdAt; // 생성 시간
    private LocalDateTime lastModifiedAt; // 처리 시간
    private LocalDateTime expirationDateTime; // 유효 기간
    private CouponTransactionEnum status; // 처리 현황
}
