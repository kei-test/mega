package GInternational.server.api.service;

import GInternational.server.api.dto.IpReqDTO;
import GInternational.server.api.entity.Ip;
import GInternational.server.api.repository.IpRepository;
import GInternational.server.api.utilities.AuditContext;
import GInternational.server.api.utilities.AuditContextHolder;
import GInternational.server.common.exception.ExceptionCode;
import GInternational.server.common.exception.RestControllerException;
import GInternational.server.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional(value = "clientServerTransactionManager")
@RequiredArgsConstructor
public class IpService {

    private final IpRepository repository;

    /**
     * IP를 차단. 요청된 IP 정보를 받아 새로운 IP 엔티티를 생성하고 저장.
     *
     * @param ipReqDTO 차단할 IP의 정보를 담고 있는 DTO
     * @param principalDetails 사용자 인증 정보
     * @return 저장된 Ip 엔티티
     */
    @AuditLogService.Audit("아이피 차단")
    public Ip blockIp(IpReqDTO ipReqDTO, PrincipalDetails principalDetails, HttpServletRequest request) {
        if (ipReqDTO.getIpContent() == null || ipReqDTO.getIpContent().trim().isEmpty()) {
            throw new RestControllerException(ExceptionCode.INVALID_REQUEST, "아이피를 입력하세요");
        }
        if (ipReqDTO.getNote() == null || ipReqDTO.getNote().trim().isEmpty()) {
            throw new RestControllerException(ExceptionCode.INVALID_REQUEST, "비고를 입력하세요");
        }

        Ip ip = new Ip();
        ip.setIpContent(ipReqDTO.getIpContent());
        ip.setNote(ipReqDTO.getNote());
        ip.setEnabled(true);

        AuditContext context = AuditContextHolder.getContext();
        String clientIp = request.getRemoteAddr();
        context.setIp(clientIp);
        context.setTargetId(null);
        context.setUsername(null);
        context.setDetails(ipReqDTO.getIpContent() + " 아이피 차단");
        context.setAdminUsername(principalDetails.getUsername());
        context.setTimestamp(LocalDateTime.now());

        return repository.save(ip);
    }

    /**
     * 차단된 모든 IP 정보를 페이지네이션하여 조회.
     *
     * @param page 조회할 페이지 번호
     * @param size 페이지 당 항목 수
     * @param principalDetails 사용자 인증 정보
     * @return 조회된 Ip 엔티티의 페이지
     */
    @Transactional(value = "clientServerTransactionManager",readOnly = true)
    public Page<Ip> findAllIp(int page, int size, PrincipalDetails principalDetails) {
        return repository.findAll(PageRequest.of(page -1,size, Sort.by("id").descending()));
    }

    /**
     * IP 차단 해제.
     *
     * @param id 차단 해제할 IP의 ID
     * @param principalDetails 사용자 인증 정보
     */
    @AuditLogService.Audit("아이피 차단 해제")
    public void deleteIp(Long id, PrincipalDetails principalDetails, HttpServletRequest request) {;
        Ip ip = validateIp(id);

        AuditContext context = AuditContextHolder.getContext();
        String clientIp = request.getRemoteAddr();
        context.setIp(clientIp);
        context.setTargetId(null);
        context.setUsername(null);
        context.setDetails(ip.getIpContent() + " 아이피 차단 해제");
        context.setAdminUsername(principalDetails.getUsername());
        context.setTimestamp(LocalDateTime.now());

        repository.delete(ip);
    }

    /**
     * 주어진 ID에 해당하는 IP가 존재하는지 검증하고, 존재한다면 해당 IP 엔티티를 반환.
     *
     * @param id 검증할 IP의 ID
     * @return 검증된 Ip 엔티티
     */
    public Ip validateIp(Long id) {
        Optional<Ip> ip = repository.findById(id);
        Ip findIp = ip.orElseThrow(()-> new RuntimeException("해당 ip 없음"));
        return findIp;
    }

    /**
     * 지정된 날짜 범위 내에 생성된 모든 IP를 조회. 결과는 페이지네이션되어 반환.
     *
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @param page 조회할 페이지 번호
     * @param size 페이지 당 항목 수
     * @param principalDetails 사용자 인증 정보
     * @return 조회된 Ip 엔티티의 페이지
     */
    @Transactional(value = "clientServerTransactionManager",readOnly = true)
    public Page<Ip> findIpsByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size, PrincipalDetails principalDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return repository.findAllByCreatedAtBetween(startDate, endDate, pageable);
    }
}
