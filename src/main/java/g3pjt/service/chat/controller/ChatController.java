package g3pjt.service.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import g3pjt.service.chat.domain.ChatDocument;
import g3pjt.service.chat.domain.ChatRoom;
import g3pjt.service.chat.dto.AddMemberRequest;
import g3pjt.service.chat.dto.ChatRoomRequest;
import g3pjt.service.chat.dto.ChatRoomMembersResponse;
import g3pjt.service.chat.dto.ChatRoomSummaryResponse;
import g3pjt.service.chat.dto.ChatUserSearchResponse;
import g3pjt.service.chat.dto.InviteLinkResponse;
import g3pjt.service.chat.dto.JoinRoomRequest;
import g3pjt.service.chat.service.ChatService;
import g3pjt.service.user.User;
import g3pjt.service.user.UserService;

import java.util.List;
import java.util.Map;

@Tag(name = "Chat API", description = "채팅 내역 조회 API")
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @Value("${FRONTEND_URL:}")
    private String frontendUrl;

    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다.")
    @PostMapping("/rooms")
    public ChatRoom createRoom(
            @RequestBody ChatRoomRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        return chatService.createRoom(request, authentication);
    }

    @Operation(summary = "유저 검색(멤버 추가용)", description = "닉네임/이메일(=username)/아이디(username)로 유저를 검색해 표시 정보를 반환합니다.")
    @GetMapping("/users/search")
    public ResponseEntity<ChatUserSearchResponse> searchUser(
            @RequestParam String identifier,
            org.springframework.security.core.Authentication authentication
    ) {
        // 인증은 SecurityConfig(/api/chats/**)에서 강제되지만, 혹시 몰라 NPE 방지
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        return userService.findOptionalByUsernameOrNickname(identifier)
                .map(user -> ResponseEntity.ok(toSearchResponse(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ChatUserSearchResponse toSearchResponse(User user) {
        Long userId = user.getId();
        return ChatUserSearchResponse.builder()
                .userId(userId)
                .displayName(userService.getDisplayNameByUserId(userId))
                .username(user.getUsername())
                .build();
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

    @Operation(summary = "채팅방 멤버/인원 조회", description = "채팅방에 참여한 멤버 목록과 인원 수를 조회합니다. (방 멤버만 가능)")
    @GetMapping("/rooms/{roomId}/members")
    public ChatRoomMembersResponse getRoomMembers(
            @PathVariable Long roomId,
            org.springframework.security.core.Authentication authentication
    ) {
        return chatService.getRoomMembers(roomId, authentication);
    }

    @Operation(summary = "채팅 이미지 업로드", description = "채팅방에 업로드할 이미지를 Supabase Storage에 업로드하고 public URL을 반환합니다. (방 멤버만 가능)")
    @PostMapping(value = "/rooms/{roomId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadChatImage(
            @PathVariable Long roomId,
            @RequestPart("file") MultipartFile file,
            org.springframework.security.core.Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String imageUrl = chatService.uploadChatImage(roomId, file, authentication);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @Operation(summary = "채팅방 삭제(방폭파)", description = "채팅방을 삭제하고 채팅 내역을 모두 삭제합니다. (방장만 가능)")
    @DeleteMapping("/rooms/{roomId}")
    public void deleteRoom(
            @PathVariable Long roomId,
            org.springframework.security.core.Authentication authentication
    ) {
        chatService.deleteRoom(roomId, authentication);
    }

    @Operation(summary = "채팅방 나가기", description = "채팅방은 유지하고, 본인만 채팅방 멤버에서 제외됩니다. (방 멤버만 가능)")
    @DeleteMapping("/rooms/{roomId}/leave")
    public ChatRoom leaveRoom(
            @PathVariable Long roomId,
            org.springframework.security.core.Authentication authentication
    ) {
        return chatService.leaveRoom(roomId, authentication);
    }

    @Operation(summary = "내 채팅방 목록 조회", description = "내가 참여 중인 채팅방 목록을 조회합니다.")
    @GetMapping("/rooms")
    public List<ChatRoom> getMyRooms(org.springframework.security.core.Authentication authentication) {
        return chatService.getMyRooms(authentication);
    }

    @Operation(summary = "내 채팅방 목록(안읽은 개수 포함)", description = "내가 참여 중인 채팅방 목록과 방별 안읽은 메시지 개수를 조회합니다.")
    @GetMapping("/rooms/summary")
    public ResponseEntity<List<ChatRoomSummaryResponse>> getMyRoomSummaries(
            org.springframework.security.core.Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(chatService.getMyRoomSummaries(authentication));
    }

    @Operation(summary = "채팅방 읽음 처리", description = "특정 채팅방을 '지금 시점까지 읽음'으로 처리하여 안읽은 개수를 0으로 만듭니다. (방 멤버만 가능)")
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markRoomAsRead(
            @PathVariable Long roomId,
            org.springframework.security.core.Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        try {
            chatService.markRoomAsRead(roomId, authentication);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @Operation(summary = "채팅 내역 조회", description = "특정 채팅방의 지난 대화 내용을 조회합니다.")
    @GetMapping("/{chatRoomId}")
    public List<ChatDocument> getChatHistory(@PathVariable Long chatRoomId) {
        return chatService.getChatHistory(chatRoomId);
    }
}
