package g3pjt.service.Ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI API", description = "AI 관련 API")
public class AiController {

    private final AiService aiService;

    @Operation(summary = "채팅방 키워드 추출", description = "특정 채팅방의 대화 내용을 분석하여 여행지 키워드를 추출합니다.")
    @PostMapping("/keywords/{chatRoomId}")
    public ResponseEntity<AiDto> extractKeywords(@PathVariable Long chatRoomId) {
        AiDto result = aiService.extractKeywords(chatRoomId);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "여행 계획 생성", description = "추출된 키워드(여행지)를 바탕으로 최적의 여행 계획을 생성합니다.")
    @PostMapping("/plan")
    public ResponseEntity<AiPlanDto> generateTravelPlan(@RequestBody AiDto aiDto) {
        AiPlanDto result = aiService.generateTravelPlan(aiDto.getKeywords());
        return ResponseEntity.ok(result);
    }
}
