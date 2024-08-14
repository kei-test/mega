package GInternational.server.api.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponNameEnum {

    MONEY_COUPON("머니쿠폰"),
    LUCKY_LOTTERY("행운복권"),
    DIRECT_MONEY("머니지급");

    private String value;
}
