package GInternational.server.api.service;

import GInternational.server.common.exception.ExceptionCode;
import GInternational.server.common.exception.RestControllerException;
import GInternational.server.api.dto.EventsBoardListDTO;
import GInternational.server.api.dto.EventsBoardRequestDTO;
import GInternational.server.api.dto.EventsBoardResponseDTO;
import GInternational.server.api.entity.EventsBoard;
import GInternational.server.api.mapper.EventsBoardListResponseMapper;
import GInternational.server.api.mapper.EventsBoardRequestMapper;
import GInternational.server.api.mapper.EventsBoardResponseMapper;
import GInternational.server.api.repository.EventsBoardRepository;

import GInternational.server.api.utilities.AuditContext;
import GInternational.server.api.utilities.AuditContextHolder;
import GInternational.server.security.auth.PrincipalDetails;
import GInternational.server.api.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(value = "clientServerTransactionManager")
@RequiredArgsConstructor
public class EventsBoardService {

    private final EventsBoardRequestMapper eventsBoardRequestMapper;
    private final EventsBoardResponseMapper eventsBoardResponseMapper;
    private final EventsBoardRepository eventsBoardRepository;
    private final UserService userService;

    /**
     * 이벤트 생성
     *
     * @param eventsBoardRequestDTO  이벤트 상세 정보가 담긴 DTO
     * @param principalDetails  현재 로그인한 사용자의 보안 상세 정보
     * @return                  저장된 이벤트의 DTO
     */
    @AuditLogService.Audit("이벤트 게시글 생성")
    public EventsBoardResponseDTO insertEvent(@Valid EventsBoardRequestDTO eventsBoardRequestDTO, PrincipalDetails principalDetails, HttpServletRequest request) {
        validateEventDates(eventsBoardRequestDTO.getStartDate(), eventsBoardRequestDTO.getEndDate());
        validateEventData(eventsBoardRequestDTO);
        User user = userService.validateUser(principalDetails.getUser().getId());
        EventsBoard eventsBoard = eventsBoardRequestMapper.toEntity(eventsBoardRequestDTO);
        eventsBoard.setWriter(user);
        eventsBoard.setCreatedAt(LocalDateTime.now());
        EventsBoard savedEvent = eventsBoardRepository.save(eventsBoard);

        AuditContext context = AuditContextHolder.getContext();
        String clientIp = request.getRemoteAddr();
        context.setIp(clientIp);
        context.setTargetId(null);
        context.setUsername(null);
        context.setDetails("이벤트 게시글 생성, 게시글 제목: " + eventsBoardRequestDTO.getTitle());
        context.setAdminUsername(principalDetails.getUsername());
        context.setTimestamp(LocalDateTime.now());

        return eventsBoardResponseMapper.toDto(savedEvent);
    }

    /**
     * 이벤트 수정
     *
     * @param eventId           업데이트할 이벤트 ID
     * @param eventsBoardRequestDTO  업데이트할 이벤트 정보가 담긴 DTO
     * @param principalDetails  현재 로그인한 사용자의 보안 상세 정보
     * @return                  업데이트된 이벤트의 DTO
     */
    @AuditLogService.Audit("이벤트 게시글 수정")
    public EventsBoardResponseDTO updateEvent(Long eventId, EventsBoardRequestDTO eventsBoardRequestDTO,
                                              PrincipalDetails principalDetails, HttpServletRequest request) {
        EventsBoard eventsBoard = validateEvent(eventId);
        validateEventDates(eventsBoardRequestDTO.getStartDate(), eventsBoardRequestDTO.getEndDate());
        validateEventData(eventsBoardRequestDTO);

        eventsBoard.setTitle(eventsBoardRequestDTO.getTitle());
        eventsBoard.setDescription(eventsBoardRequestDTO.getDescription());
        eventsBoard.setEnabled(eventsBoardRequestDTO.isEnabled());
        eventsBoard.setStartDate(eventsBoardRequestDTO.getStartDate());
        eventsBoard.setEndDate(eventsBoardRequestDTO.getEndDate());
        eventsBoard.setUpdatedAt(LocalDateTime.now());

        AuditContext context = AuditContextHolder.getContext();
        String clientIp = request.getRemoteAddr();
        context.setIp(clientIp);
        context.setTargetId(null);
        context.setUsername(null);
        context.setDetails("이벤트 게시글 수정, 게시글 제목: " + eventsBoardRequestDTO.getTitle());
        context.setAdminUsername(principalDetails.getUsername());
        context.setTimestamp(LocalDateTime.now());

        return eventsBoardResponseMapper.toDto(eventsBoardRepository.save(eventsBoard));
    }

    /**
     * 이벤트 전체 조회.
     * 게스트가 아닌 모든 사용자가 조회할 수 있음.
     *
     * @param principalDetails 현재 로그인한 사용자의 보안 상세 정보
     * @return 조회된 모든 이벤트의 DTO 목록
     */


    @Transactional(value = "clientServerTransactionManager",readOnly = true)
    public List<EventsBoardListDTO> getAllEvents(PrincipalDetails principalDetails) {

        List<EventsBoard> eventsBoardList = eventsBoardRepository.findAll();
        return eventsBoardList.stream()
                .map(EventsBoardListResponseMapper.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 주어진 이벤트 ID에 해당하는 이벤트를 조회함.
     * 조회는 GUEST 역할이 아닌 사용자에게만 허용됨.
     *
     * @param eventId          조회할 이벤트 ID
     * @param principalDetails 현재 로그인한 사용자의 보안 상세 정보
     * @return                 조회된 특정 이벤트의 DTO
     */
    @Transactional(value = "clientServerTransactionManager",readOnly = true)
    public EventsBoardResponseDTO detailEvent(Long eventId, PrincipalDetails principalDetails) {

        EventsBoard event = validateEvent(eventId);
        return eventsBoardResponseMapper.toDto(event);
    }

    /**
     * 주어진 이벤트 ID에 해당하는 이벤트를 삭제함.
     * 삭제 권한은 이벤트의 작성자 또는 관리자에게 있음.
     *
     * @param eventId          삭제할 이벤트 ID
     * @param principalDetails 현재 로그인한 사용자의 보안 상세 정보
     */
    @AuditLogService.Audit("이벤트 게시글 삭제")
    public void deleteEvent(Long eventId, PrincipalDetails principalDetails, HttpServletRequest request) {
        EventsBoard event = eventsBoardRepository.findById(eventId)
                .orElseThrow(() -> new RestControllerException(ExceptionCode.EVENT_NOT_FOUND, "이벤트를 찾을 수 없습니다."));

        AuditContext context = AuditContextHolder.getContext();
        String clientIp = request.getRemoteAddr();
        context.setIp(clientIp);
        context.setTargetId(null);
        context.setUsername(null);
        context.setDetails("이벤트 게시글 삭제, 게시글 제목: " + event.getTitle());
        context.setAdminUsername(principalDetails.getUsername());
        context.setTimestamp(LocalDateTime.now());

        eventsBoardRepository.delete(event);
    }

    /**
     * 이벤트 활성화/비활성화
     *
     * @param eventId           활성화/비활성화할 이벤트 ID
     * @param principalDetails  현재 로그인한 사용자의 보안 상세 정보
     * @param enable            활성화 상태(true for 활성화, false for 비활성화)
     * @return                  업데이트된 이벤트의 DTO
     */
    @AuditLogService.Audit("이벤트 게시글 활성화 상태 변경")
    public EventsBoardResponseDTO updateEventStatus(Long eventId, PrincipalDetails principalDetails, boolean enable, HttpServletRequest request) {
        EventsBoard event = validateEvent(eventId);
        if (event.isEnabled() == enable) {
            throw new RestControllerException(ExceptionCode.INVALID_STATUS, "유효하지 않은 상태입니다.");
        }

        event.setEnabled(enable);
        event.setUpdatedAt(LocalDateTime.now());

        AuditContext context = AuditContextHolder.getContext();
        String clientIp = request.getRemoteAddr();
        context.setIp(clientIp);
        context.setTargetId(null);
        context.setUsername(null);
        context.setDetails("이벤트 게시글 활성화 상태 변경, 게시글 제목: " + event.getTitle() + ", 활성화 상태 " + (enable ? "활성화" : "비활성화") + "로 변경");
        context.setAdminUsername(principalDetails.getUsername());
        context.setTimestamp(LocalDateTime.now());

        return eventsBoardResponseMapper.toDto(eventsBoardRepository.save(event));
    }


    /**
     * 주어진 ID를 가진 이벤트가 존재하는지 확인함.
     * 이 메서드는 이벤트의 존재 유무를 검증할 때 사용됨.
     *
     * @param id 검증할 이벤트 ID
     * @return   존재하는 이벤트
     * @throws RestControllerException 이벤트가 존재하지 않을 경우 오류 발생
     */
    private EventsBoard validateEvent(Long id) {
        return eventsBoardRepository.findById(id)
                .orElseThrow(() -> new RestControllerException(ExceptionCode.EVENT_NOT_FOUND, "이벤트를 찾을 수 없습니다."));
    }

    // 날짜 검증 메서드
    private void validateEventDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new RestControllerException(ExceptionCode.START_DATE_AFTER_END_DATE, "시작 날짜가 종료 날짜보다 늦을 수 없습니다.");
        }
        if (startDate.isBefore(LocalDateTime.now())) {
            throw new RestControllerException(ExceptionCode.START_DATE_IN_PAST, "시작 날짜가 과거일 수 없습니다.");
        }
    }

    // 데이터 검증 메서드
    private void validateEventData(EventsBoardRequestDTO requestDTO) {
        if (requestDTO.getTitle() == null || requestDTO.getTitle().trim().isEmpty()) {
            throw new RestControllerException(ExceptionCode.INVALID_EVENT_TITLE, "이벤트 제목을 입력해야 합니다.");
        }
        if (requestDTO.getDescription() == null || requestDTO.getDescription().trim().isEmpty()) {
            throw new RestControllerException(ExceptionCode.INVALID_EVENT_DESCRIPTION, "이벤트 설명을 입력해야 합니다.");
        }
        // 추가적인 데이터 검증 로직
    }
}
