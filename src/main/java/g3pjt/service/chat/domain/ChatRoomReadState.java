package g3pjt.service.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_room_read_states")
public class ChatRoomReadState {
    @Id
    private String id;

    private Long roomId;
    private Long userId;

    private Instant lastReadAt;
    private Instant updatedAt;
}
