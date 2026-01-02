package g3pjt.service.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "chat_polls")
public class ChatPollDocument {

    @Id
    private String id;

    private Long chatRoomId;

    private Long createdByUserId;
    private String createdByName;

    private String question;

    @Builder.Default
    private List<PollOption> options = new ArrayList<>();

    private Instant createdAt;

    // 투표 종료(마감) 처리
    private Instant closedAt;
    private Long closedByUserId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PollOption {
        private String optionId;
        private String text;
    }
}
