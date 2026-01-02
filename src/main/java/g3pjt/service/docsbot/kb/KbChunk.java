package g3pjt.service.docsbot.kb;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KbChunk {
    private String source; // 예: kb/tokplan_kb.md
    private String title;  // 예: "채팅 API"
    private String content;
}
