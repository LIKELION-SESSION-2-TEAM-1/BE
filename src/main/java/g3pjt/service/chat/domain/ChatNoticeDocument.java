package g3pjt.service.chat.domain;

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
@Document(collection = "chat_notices")
public class ChatNoticeDocument {

    public enum NoticeType {POLL}

    @Id
    private String id;

    private Long chatRoomId;

    private NoticeType type;

    private Long createdByUserId;
    private String createdByName;

    // For type == POLL
    private String pollId;
    private String message;

    private Instant createdAt;
}
