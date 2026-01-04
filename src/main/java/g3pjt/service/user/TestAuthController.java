package g3pjt.service.user;

import g3pjt.service.user.User;
import g3pjt.service.user.UserRepository;
import g3pjt.service.user.jwt.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Tag(name = "Test Auth API", description = "테스트용 인증 API (배포 시 주의)")
@RestController
@RequestMapping("/api/test/auth")
@RequiredArgsConstructor
public class TestAuthController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.test-auth.enabled:false}")
    private boolean enabled;

    @Operation(summary = "테스트용 토큰 발급", description = "이메일만으로 즉시 JWT 토큰을 발급받습니다. (회원가입 자동 진행)")
    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> getTestToken(@RequestParam String email) {
        if (!enabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Not Found"));
        }

        if (email == null || email.trim().isEmpty() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return ResponseEntity.badRequest().body(Map.of("message", "email 형식이 올바르지 않습니다."));
        }

        String normalizedEmail = email.trim();
        User user = userRepository.findByUsername(email)
                .orElseGet(() -> {
                    User newUser = new User(normalizedEmail, UUID.randomUUID().toString());
                    newUser.setNickname("TestUser_" + email.split("@")[0]);
                    return userRepository.save(newUser);
                });

        String token = jwtUtil.createToken(user.getUsername());
        
        return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "email", user.getUsername(),
                "nickname", user.getNickname()
        ));
    }
}
