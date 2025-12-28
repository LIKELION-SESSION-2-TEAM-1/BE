package g3pjt.service.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiDto {
    @Schema(description = "추출된 키워드 리스트", example = "[\"제주도\", \"맛집\", \"해변\"]")
    private List<String> keywords;

    @Schema(description = "채팅방 ID", example = "1")
    private Long chatRoomId;
}
