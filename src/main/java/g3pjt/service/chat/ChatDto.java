package g3pjt.service.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatDto {
    public enum MessageType{JOIN, TALK, LEAVE, DM}

    private Long chatRoomId;
    private Long senderUserId; 
    private String senderName;
    private Long receiverUserId; 
    private String receiverName;
    private String message;

    private MessageType messageType;
    private Instant ts;

}
