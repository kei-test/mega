package GInternational.server.api.controller;

import GInternational.server.api.dto.ExpRecordResponseDTO;
import GInternational.server.api.service.ExpRecordService;
import GInternational.server.api.vo.ExpRecordEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class ExpRecordController {

    private final ExpRecordService expRecordService;

    @GetMapping("/users/exp/record/all")
    public ResponseEntity<Page<ExpRecordResponseDTO>> getExpRecords(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) ExpRecordEnum content,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ExpRecordResponseDTO> records = expRecordService.findExpRecords(username, nickname, content, page, size);
        return ResponseEntity.ok(records);
    }
}
