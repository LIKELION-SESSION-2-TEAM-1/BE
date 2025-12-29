package g3pjt.service.chat.domain;

import g3pjt.service.chat.dto.ChatDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "chats")
public class ChatDocument {

    @Id
    private String id;

    private Long chatRoomId;
    private Long senderUserId;
    private String senderName;
    private Long receiverUserId;
    private String receiverName;
    private String message;

    // Optional: for poll messages
    private String pollId;

    private String imageUrl;

    private ChatDto.MessageType messageType;

    private Instant timestamp;
}
