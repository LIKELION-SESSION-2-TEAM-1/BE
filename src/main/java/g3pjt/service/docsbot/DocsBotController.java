package g3pjt.service.docsbot;

import g3pjt.service.docsbot.dto.DocsBotChatRequest;
import g3pjt.service.docsbot.dto.DocsBotChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/help")
@RequiredArgsConstructor
@Tag(name = "Docs Bot API", description = "앱/백엔드 설명용 챗봇(지식베이스 기반)")
public class DocsBotController {

    private final DocsBotService docsBotService;

    @Operation(summary = "앱 설명 챗봇", description = "프로젝트 지식베이스(md)를 근거로 답변합니다. (Gemini API 사용)")
    @PostMapping("/chat")
    public ResponseEntity<DocsBotChatResponse> chat(@RequestBody DocsBotChatRequest request) {
        return ResponseEntity.ok(docsBotService.chat(request));
    }
}
