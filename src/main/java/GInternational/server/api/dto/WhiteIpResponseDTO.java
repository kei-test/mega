package GInternational.server.api.dto;

import GInternational.server.api.vo.WhiteIpMemoStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WhiteIpResponseDTO {
    private Long id;
    private String whiteIp;
    private WhiteIpMemoStatusEnum memoStatus;
    private String memo;
}
