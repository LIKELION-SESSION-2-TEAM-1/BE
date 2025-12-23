package g3pjt.service.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_rooms")
public class ChatRoom {
    @Id
    private String id;

    private Long roomId; // Numeric ID used in ChatDocument
    private String name;
    
    // 채팅방 참여자 ID 목록 (User.id)
    @Builder.Default
    private java.util.List<Long> memberIds = new java.util.ArrayList<>();
}
