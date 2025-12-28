package g3pjt.service.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiDto {
    private List<String> keywords;
    private Long chatRoomId;
}
