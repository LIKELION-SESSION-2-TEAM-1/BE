package g3pjt.service.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "STOMP 채팅 메시지 DTO")
public class ChatDto {
    public enum MessageType{JOIN, TALK, LEAVE, DM, IMAGE, POLL}

    private Long chatRoomId;
    private Long senderUserId; 
    private String senderName;
    private Long receiverUserId; 
    private String receiverName;
    private String message;

    // Optional: when messageType == POLL
    @Schema(description = "투표 ID (messageType == POLL 일 때 사용)")
    private String pollId;

    private String imageUrl;

    private MessageType messageType;

    @JsonAlias({"ts"})
    private Instant timestamp;

}
