package g3pjt.service.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "채팅방 투표 생성 요청")
public class CreatePollRequest {
    @Schema(description = "투표 질문", example = "점심 뭐 먹을까?")
    private String question;

    @Schema(description = "투표 항목(최소 2개)", example = "[\"국밥\", \"파스타\", \"샐러드\"]")
    private List<String> options;
}
