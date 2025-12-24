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

    @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL:}")
    private String frontendUrl;

    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다.")
    @PostMapping("/rooms")
    public ChatRoom createRoom(
            @RequestBody ChatRoomRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        return chatService.createRoom(request, authentication);
    }

    @Operation(summary = "채팅방 멤버 추가", description = "닉네임/이메일(=username)/아이디(username)로 채팅방 멤버를 추가합니다. (방 멤버만 가능)")
    @PostMapping("/rooms/{roomId}/members")
    public ChatRoom addMember(
            @PathVariable Long roomId,
            @RequestBody AddMemberRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        return chatService.addMemberByIdentifier(roomId, request.getIdentifier(), authentication);
    }

    @Operation(summary = "초대 링크 생성", description = "채팅방 초대 링크(코드)를 생성합니다. (방 멤버만 가능)")
    @PostMapping("/rooms/{roomId}/invite-link")
    public InviteLinkResponse createInviteLink(
            @PathVariable Long roomId,
            org.springframework.security.core.Authentication authentication
    ) {
        return chatService.generateInviteLink(roomId, authentication, frontendUrl);
    }

    @Operation(summary = "초대 코드로 채팅방 참가", description = "초대 코드를 이용해 채팅방에 참가합니다.")
    @PostMapping("/rooms/{roomId}/join")
    public ChatRoom joinRoom(
            @PathVariable Long roomId,
            @RequestBody JoinRoomRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        return chatService.joinByInviteCode(roomId, request.getInviteCode(), authentication);
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
