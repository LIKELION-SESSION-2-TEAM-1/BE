package g3pjt.service.docsbot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DocsBotChatResponse {
    private String answer;
    private List<Source> sources;

    @Getter
    @AllArgsConstructor
    public static class Source {
        private String source;
        private String title;
    }
}
