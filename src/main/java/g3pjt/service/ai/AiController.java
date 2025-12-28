package g3pjt.service.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import g3pjt.service.user.User;
import g3pjt.service.user.UserService;
import g3pjt.service.ai.domain.UserTravelPlan;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI API", description = "AI 관련 API")
public class AiController {

    private final AiService aiService;
    private final UserService userService;

    @Operation(summary = "채팅방 키워드 추출", description = "특정 채팅방의 대화 내용을 분석하여 여행지 키워드를 추출합니다.")
    @PostMapping("/keywords/{chatRoomId}")
    public ResponseEntity<AiDto> extractKeywords(@PathVariable Long chatRoomId) {
        AiDto result = aiService.extractKeywords(chatRoomId);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "여행 계획 생성", description = "추출된 키워드(여행지)를 바탕으로 최적의 여행 계획을 생성합니다.")
    @PostMapping("/plan")
    public ResponseEntity<AiPlanDto> generateTravelPlan(@RequestBody AiDto aiDto) {
        AiPlanDto result = aiService.generateTravelPlan(aiDto.getChatRoomId(), aiDto.getKeywords());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "여행 계획 확정", description = "생성되거나 수정된 여행 계획을 사용자의 계획으로 확정(저장)합니다.")
    @PostMapping("/plan/confirm")
    public ResponseEntity<UserTravelPlan> confirmTravelPlan(
            @RequestBody AiPlanDto aiPlanDto,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        User user;
        try {
            user = userService.getUserProfile(username);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).build();
        }

        UserTravelPlan result = aiService.confirmPlan(user.getId(), aiPlanDto);
        return ResponseEntity.ok(result);
    }
}
