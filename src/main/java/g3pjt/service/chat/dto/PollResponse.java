package g3pjt.service.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "채팅방 투표 응답(결과 포함)")
public class PollResponse {

    @Schema(description = "투표 ID")
    private String pollId;

    @Schema(description = "채팅방 ID")
    private Long chatRoomId;

    @Schema(description = "투표 생성자 User ID")
    private Long createdByUserId;

    @Schema(description = "투표 생성자 표시명")
    private String createdByName;

    @Schema(description = "투표 질문")
    private String question;

    @Schema(description = "투표 항목 및 득표수")
    private List<PollOptionResult> options;

    @Schema(description = "내가 투표한 optionId (아직 투표 안 했으면 null)")
    private String myVotedOptionId;

    @Schema(description = "생성 시각")
    private Instant createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "투표 항목 결과")
    public static class PollOptionResult {
        @Schema(description = "옵션 ID")
        private String optionId;

        @Schema(description = "옵션 텍스트")
        private String text;

        @Schema(description = "득표수")
        private long voteCount;
    }
}
