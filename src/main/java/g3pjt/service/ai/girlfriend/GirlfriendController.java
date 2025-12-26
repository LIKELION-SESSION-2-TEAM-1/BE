package g3pjt.service.ai.girlfriend;

import g3pjt.service.ai.girlfriend.dto.GirlfriendChatRequest;
import g3pjt.service.ai.girlfriend.dto.GirlfriendChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/girlfriend")
@RequiredArgsConstructor
@Tag(name = "Virtual Girlfriend API", description = "여행 분위기를 내주는 가상 여자친구(여름이) API")
public class GirlfriendController {

    private final GirlfriendService girlfriendService;

    @Operation(summary = "여자친구와 대화하기", description = "사용자의 메시지를 보내면, 여행을 좋아하는 여자친구 '여름이'가 스윗하게 답변합니다. (GPT-4o)")
    @PostMapping("/chat")
    public ResponseEntity<GirlfriendChatResponse> chat(@RequestBody GirlfriendChatRequest request) {
        return ResponseEntity.ok(girlfriendService.chat(request));
    }
}
