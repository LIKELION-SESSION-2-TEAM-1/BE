package g3pjt.service.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Chat API", description = "채팅 내역 조회 API")
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRepository chatRepository;

    @Operation(summary = "채팅 내역 조회", description = "특정 채팅방의 지난 대화 내용을 조회합니다.")
    @GetMapping("/{chatRoomId}")
    public List<ChatDocument> getChatHistory(@PathVariable Long chatRoomId) {
        return chatRepository.findByChatRoomId(chatRoomId);
    }
}
