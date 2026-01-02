package g3pjt.service.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ChatMessageSearchResponse {
    private String messageId;
    private Long roomId;
    private String roomName;

    private Long senderUserId;
    private String senderName;

    private String message;
    private Instant timestamp;
}
