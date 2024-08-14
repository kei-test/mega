package GInternational.server.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminListUpdateDTO {
    private String approveIp; // 로그인이 승인된 IP 주소
    private boolean isBlockedAdmin; // 관리자 계정의 상태 설정 (예: true:사용중, false:사용불가) - 사용불가이면 로그인 차단 - "로그인이 차단된 계정입니다"
    private String password;

    public boolean getIsBlockedAdmin() {
        return this.isBlockedAdmin;
    }
}
