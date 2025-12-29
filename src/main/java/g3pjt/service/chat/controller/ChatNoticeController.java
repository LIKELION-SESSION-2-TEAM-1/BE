package g3pjt.service.chat.controller;

import g3pjt.service.chat.dto.CreatePollNoticeRequest;
import g3pjt.service.chat.dto.NoticeResponse;
import g3pjt.service.chat.service.ChatNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat Notice API", description = "채팅방 공지 API")
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatNoticeController {

    private final ChatNoticeService chatNoticeService;

    @Operation(summary = "투표를 공지로 등록", description = "채팅방 공지에 투표를 등록합니다. (방 멤버면 누구나 가능)")
    @PostMapping("/rooms/{roomId}/notices/polls")
    public NoticeResponse addPollNotice(
            @PathVariable Long roomId,
            @RequestBody CreatePollNoticeRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        return chatNoticeService.addPollNotice(roomId, request, authentication);
    }

    @Operation(summary = "공지 목록 조회", description = "채팅방 공지 목록을 조회합니다. (방 멤버만 가능)")
    @GetMapping("/rooms/{roomId}/notices")
    public List<NoticeResponse> listNotices(
            @PathVariable Long roomId,
            org.springframework.security.core.Authentication authentication
    ) {
        return chatNoticeService.listNotices(roomId, authentication);
    }
}
