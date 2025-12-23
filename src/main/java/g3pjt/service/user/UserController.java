package g3pjt.service.user;

import g3pjt.service.user.jwt.JwtUtil;
import g3pjt.service.user.userdto.LoginRequestDto;
import g3pjt.service.user.userdto.SignupRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User API", description = "회원 관련 API")
@RestController // JSON 응답을 위한 컨트롤러
@RequestMapping("/api/user") // 공통 URL 접두사
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil; // JWT 토큰 생성을 위해 주입

    /**
     * 회원가입 API
     */
    @Operation(summary = "회원가입", description = "자체 회원가입을 진행합니다.")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequestDto requestDto) {
        userService.signup(requestDto);
        // 성공 시 HTTP 201 Created 상태와 메시지 반환
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공");
    }

    /**
     * 로그인 API
     */
    @Operation(summary = "로그인", description = "자체 로그인을 통해 JWT 토큰을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<java.util.Map<String, String>> login(@RequestBody LoginRequestDto requestDto) {
        // 1. UserService에서 로그인 시도 및 토큰 발급
        String token = userService.login(requestDto);

        // 2. 토큰을 Response Header 및 Body에 모두 포함
        java.util.Map<String, String> responseBody = new java.util.HashMap<>();
        responseBody.put("message", "로그인 성공");
        responseBody.put("token", token);

        return ResponseEntity.ok()
                .header(JwtUtil.AUTHORIZATION_HEADER, token)
                .body(responseBody);
    }


    /**
     * 구글 로그인 (Swagger 설명용)
     */
    @Operation(summary = "구글 로그인", description = "구글 OAuth2 인증 페이지로 리다이렉트합니다. (브라우저에서 직접 호출)")
    @GetMapping("/google/login")
    public ResponseEntity<Void> googleLogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/oauth2/authorization/google")
                .build();
    }

    /**
     * 내 프로필 조회
     */
    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 상세 정보를 조회합니다.")
    @GetMapping("/profile")
    public ResponseEntity<User> getMyProfile(org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserProfile(username);
        return ResponseEntity.ok(user);
    }

    /**
     * 내 프로필 수정
     */
    @Operation(summary = "내 프로필 수정", description = "현재 로그인한 사용자의 프로필 정보(닉네임, 여행스타일 등)를 수정합니다.")
    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(
            org.springframework.security.core.Authentication authentication,
            @RequestBody g3pjt.service.user.userdto.UserUpdateRequest requestDto) {
        String username = authentication.getName();
        userService.updateProfile(username, requestDto);
        return ResponseEntity.ok("프로필 수정 완료");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
