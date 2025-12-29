package g3pjt.service.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import g3pjt.service.user.User;
import g3pjt.service.user.UserService;
import g3pjt.service.ai.domain.UserTravelPlan;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import java.util.List;
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

    @Operation(summary = "여행 계획 수정 (체크/완료/삭제 반영)", description = "기존에 확정된 여행 계획을 수정합니다. (일정 삭제, 완료 체크 등 프론트에서 수정된 전체 JSON을 보냄)")
    @PutMapping("/plan/{planId}")
    public ResponseEntity<UserTravelPlan> updateTravelPlan(
            @PathVariable String planId,
            @RequestBody AiPlanDto updatedPlan,
            Authentication authentication
    ) {
        User user = validateAndGetUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();

        try {
            UserTravelPlan result = aiService.updateTravelPlan(planId, user.getId(), updatedPlan);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).build(); // or 404 based on exception message
        }
    }

    @Operation(summary = "내 여행 계획 목록 조회", description = "내가 저장한 모든 여행 계획을 조회합니다.")
    @GetMapping("/plans")
    public ResponseEntity<List<UserTravelPlan>> getMyPlans(
            @RequestParam(required = false) Long chatRoomId,
            Authentication authentication
    ) {
        User user = validateAndGetUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(aiService.getUserPlans(user.getId(), chatRoomId));
    }

    @Operation(summary = "여행 계획 삭제", description = "저장된 여행 계획을 삭제합니다.")
    @DeleteMapping("/plan/{planId}")
    public ResponseEntity<Void> deletePlan(
            @PathVariable String planId,
            Authentication authentication
    ) {
        User user = validateAndGetUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();

        try {
            aiService.deletePlan(planId, user.getId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).build();
        }
    }

    private User validateAndGetUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        try {
            return userService.getUserProfile(username);
        } catch (Exception e) {
            return null;
        }
    }
}
