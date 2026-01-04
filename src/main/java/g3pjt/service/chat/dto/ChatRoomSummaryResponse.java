package g3pjt.service.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;

@Getter
@Builder
public class ChatRoomSummaryResponse {
    private Long roomId;
    private String name;

    private String imageUrl;

    private LocalDate startDate;
    private LocalDate endDate;
    private String travelStyle;
    private LocalDateTime createdAt;

    private Long ownerUserId;

    private long unreadCount;

    private Instant lastMessageTime;

    private String lastMessagePreview;
}
