package g3pjt.service.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    private LocalDate startDate;
    private LocalDate endDate;
    private String travelStyle;
    private LocalDateTime createdAt;

    private Long ownerUserId;

    private String inviteCode;
    private LocalDateTime inviteCodeCreatedAt;
    
    // 채팅방 참여자 ID 목록 (User.id)
    @Builder.Default
    private java.util.List<Long> memberIds = new java.util.ArrayList<>();
}
