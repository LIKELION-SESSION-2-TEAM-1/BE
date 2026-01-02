package g3pjt.service.chat.controller;

import g3pjt.service.chat.dto.CreatePollRequest;
import g3pjt.service.chat.dto.PollResponse;
import g3pjt.service.chat.dto.VotePollRequest;
import g3pjt.service.chat.service.ChatPollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat Poll API", description = "채팅방 투표 API")
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatPollController {

    private final ChatPollService chatPollService;

    @Operation(summary = "투표 생성", description = "채팅방에 투표를 생성하고, POLL 메시지를 브로드캐스트합니다. (방 멤버만 가능)")
    @PostMapping("/rooms/{roomId}/polls")
    public PollResponse createPoll(
            @PathVariable Long roomId,
            @RequestBody CreatePollRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        return chatPollService.createPoll(roomId, request, authentication);
    }

    @Operation(summary = "투표 목록 조회", description = "채팅방의 투표 목록을 조회합니다. (방 멤버만 가능)")
    @GetMapping("/rooms/{roomId}/polls")
    public List<PollResponse> listPolls(
            @PathVariable Long roomId,
            org.springframework.security.core.Authentication authentication
    ) {
        return chatPollService.listPolls(roomId, authentication);
    }

    @Operation(summary = "투표 상세 조회", description = "투표 상세/결과를 조회합니다. (방 멤버만 가능)")
    @GetMapping("/polls/{pollId}")
    public PollResponse getPoll(
            @PathVariable String pollId,
            org.springframework.security.core.Authentication authentication
    ) {
        return chatPollService.getPoll(pollId, authentication);
    }

    @Operation(summary = "투표하기", description = "투표 항목에 투표합니다. (방 멤버만 가능, 1인 1표)")
    @PostMapping("/polls/{pollId}/votes")
    public ResponseEntity<PollResponse> vote(
            @PathVariable String pollId,
            @RequestBody VotePollRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        return ResponseEntity.ok(chatPollService.vote(pollId, request, authentication));
    }

    @Operation(summary = "투표 종료", description = "투표를 종료(마감)합니다. (투표 생성자만 가능)")
    @PostMapping("/polls/{pollId}/close")
    public ResponseEntity<PollResponse> closePoll(
            @PathVariable String pollId,
            org.springframework.security.core.Authentication authentication
    ) {
        return ResponseEntity.ok(chatPollService.closePoll(pollId, authentication));
    }
}
