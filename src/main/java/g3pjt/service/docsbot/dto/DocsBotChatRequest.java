package g3pjt.service.docsbot.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DocsBotChatRequest {
    private String question;

    /**
     * 선택: 이전 대화(최신이 뒤).
     * 예: ["사용자: ...", "봇: ..."]
     */
    private List<String> chatHistory;
}
