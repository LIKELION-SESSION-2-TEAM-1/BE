package g3pjt.service.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "채팅방 공지 응답")
public class NoticeResponse {

    @Schema(description = "공지 ID")
    private String noticeId;

    @Schema(description = "채팅방 ID")
    private Long chatRoomId;

    @Schema(description = "공지 타입", example = "POLL")
    private String type;

    @Schema(description = "작성자 User ID")
    private Long createdByUserId;

    @Schema(description = "작성자 표시명")
    private String createdByName;

    @Schema(description = "투표 ID (type==POLL)")
    private String pollId;

    @Schema(description = "공지 메시지")
    private String message;

    @Schema(description = "생성 시각")
    private Instant createdAt;
}
