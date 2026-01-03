package g3pjt.service.user.email;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Email Verification API", description = "회원가입 유저 이메일 인증 API")
@RestController
@RequestMapping("/api/user/email/verification")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "인증 메일 발송", description = "로그인한 자체 가입 유저에게 이메일 인증 메일을 발송합니다.")
    @PostMapping("/send")
    public ResponseEntity<Void> send(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        try {
            emailVerificationService.sendVerificationEmail(authentication);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            // 이미 인증됨/이메일 아님 등
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "인증 메일 재발송(비로그인)", description = "username(이메일)을 받아 이메일 인증 메일을 재발송합니다. (개발 편의용: 스팸 방지를 위한 레이트리밋/캡차가 필요할 수 있습니다.)")
    @PostMapping("/resend")
    public ResponseEntity<java.util.Map<String, String>> resend(@RequestBody EmailVerificationResendRequest request) {
        try {
            emailVerificationService.sendVerificationEmailByUsername(request != null ? request.getUsername() : null);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "이메일 인증 확정", description = "메일로 받은 token을 서버에 전달해 이메일 인증을 완료합니다.")
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirm(@RequestBody EmailVerificationConfirmRequest request) {
        try {
            emailVerificationService.confirm(request != null ? request.getToken() : null);
            return ResponseEntity.ok(Map.of("message", "이메일 인증 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "이메일 인증 확정(GET)", description = "메일 링크용: token 쿼리로 이메일 인증을 완료합니다.")
    @GetMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmGet(@RequestParam String token) {
        try {
            emailVerificationService.confirm(token);
            return ResponseEntity.ok(Map.of("message", "이메일 인증 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
