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

    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final g3pjt.service.user.UserService userService;

    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다.")
    @PostMapping("/rooms")
    public ChatRoom createRoom(@RequestParam String name, org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        g3pjt.service.user.User user = userService.getUserProfile(username);

        Long roomId = System.currentTimeMillis(); // Simple ID generation
        
        // 채팅방 생성 시 본인(방장) 자동 참여
        java.util.List<Long> members = new java.util.ArrayList<>();
        members.add(user.getId());

        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(roomId)
                .name(name)
                .memberIds(members)
                .build();
        return chatRoomRepository.save(chatRoom);
    }

    @Operation(summary = "내 채팅방 목록 조회", description = "내가 참여 중인 채팅방 목록을 조회합니다.")
    @GetMapping("/rooms")
    public List<ChatRoom> getMyRooms(org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        g3pjt.service.user.User user = userService.getUserProfile(username);
        
        // 내가 포함된 방만 조회
        return chatRoomRepository.findByMemberIdsContains(user.getId());
    }

    @Operation(summary = "채팅 내역 조회", description = "특정 채팅방의 지난 대화 내용을 조회합니다.")
    @GetMapping("/{chatRoomId}")
    public List<ChatDocument> getChatHistory(@PathVariable Long chatRoomId) {
        return chatRepository.findByChatRoomId(chatRoomId);
    }
}
