package GInternational.server.api.dto;

import GInternational.server.api.vo.UserGubunEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminRequestDTO {


    private String username;
    private UserGubunEnum userGubunEnum;
    private String password;
    private String role;
    private String name;
    private String nickname;
    private String phone;
    private String approveIp;
    private String distributor; // 총판 구분을 위한 필드값. 누구에게 가입되었는지를 의미. (예: 윈드, 메가, 기타 총판 등등 최상위 값)
}
