package g3pjt.service.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList; // List 초기화를 위해 추가
import java.util.List;      // List 타입을 위해 추가

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

    private String imageUrl;

    private LocalDate startDate;
    private LocalDate endDate;

    private String travelStyle;

    // AiService에서 getStyles()를 호출할 때 사용되는 필드입니다.
    // MongoDB에는 ["힐링", "먹방"] 형태의 배열로 저장됩니다.
    @Builder.Default
    private List<String> styles = new ArrayList<>();

    private LocalDateTime createdAt;

    private Long ownerUserId;

    private String inviteCode;
    private LocalDateTime inviteCodeCreatedAt;

    // 채팅방 참여자 ID 목록 (User.id)
    @Builder.Default
    private List<Long> memberIds = new ArrayList<>();
}