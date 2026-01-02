package g3pjt.service.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "공지에 투표 올리기 요청")
public class CreatePollNoticeRequest {

    @Schema(description = "공지로 올릴 pollId")
    private String pollId;

    @Schema(description = "공지 메시지(선택)", example = "투표 참여 부탁!")
    private String message;
}
