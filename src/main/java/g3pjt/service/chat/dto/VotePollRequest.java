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
@Schema(description = "채팅방 투표 투표하기 요청")
public class VotePollRequest {
    @Schema(description = "선택한 옵션 ID (투표 생성 응답/상세 응답의 options[].optionId)")
    private String optionId;
}
