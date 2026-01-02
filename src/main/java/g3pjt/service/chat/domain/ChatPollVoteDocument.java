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
@Document(collection = "chat_poll_votes")
public class ChatPollVoteDocument {

    @Id
    private String id; // pollId:userId

    private String pollId;
    private Long chatRoomId;

    private Long userId;
    private String optionId;

    private Instant createdAt;
}
