package g3pjt.service.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ChatRoomSearchResponse {
    private Long roomId;
    private String name;

    private String lastMessage;
    private Instant lastMessageAt;
}
