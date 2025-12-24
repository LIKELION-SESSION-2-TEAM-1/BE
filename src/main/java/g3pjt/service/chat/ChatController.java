package g3pjt.service.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat API", description = "채팅 내역 조회 API")
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다.")
    @PostMapping("/rooms")
    public ChatRoom createRoom(
            @RequestBody ChatRoomRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        return chatService.createRoom(request, authentication);
    }

    @Operation(summary = "내 채팅방 목록 조회", description = "내가 참여 중인 채팅방 목록을 조회합니다.")
    @GetMapping("/rooms")
    public List<ChatRoom> getMyRooms(org.springframework.security.core.Authentication authentication) {
        return chatService.getMyRooms(authentication);
    }

    @Operation(summary = "채팅 내역 조회", description = "특정 채팅방의 지난 대화 내용을 조회합니다.")
    @GetMapping("/{chatRoomId}")
    public List<ChatDocument> getChatHistory(@PathVariable Long chatRoomId) {
        return chatService.getChatHistory(chatRoomId);
    }
}
