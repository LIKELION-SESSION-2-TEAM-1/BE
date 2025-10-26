package g3pjt.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StompChatController {

    private final SimpMessagingTemplate template;
    // 클라이언트 SEND: /pub/chat/{chatRoomId}
    // 클라이언트 SUB: /sub/chat/{chatRoomId}
    @MessageMapping("/chat/{chatRoomId}")
    public void sendToRoom(@DestinationVariable Long chatRoomId, @Payload ChatDto dto) {
        dto.setTs(Instant.now());

        log.info("[Room {}] {} -> {}", chatRoomId, dto.getChatRoomId(), dto.getMessage());
        template.convertAndSend("/sub/chat/" + chatRoomId, dto);

        if (dto.getMessageType() == ChatDto.MessageType.DM
            && dto.getReceiverUserId() != null){
            template.convertAndSendToUser(
                    String.valueOf(dto.getReceiverUserId()),
                    "/queue/dm",
                    dto
            );
        }
    }
}
