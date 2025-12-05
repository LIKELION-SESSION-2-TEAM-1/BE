package g3pjt.service.user;

import g3pjt.service.user.jwt.JwtUtil;
import g3pjt.service.user.userdto.LoginRequestDto;
import g3pjt.service.user.userdto.SignupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // JSON 응답을 위한 컨트롤러
@RequestMapping("/api/user") // 공통 URL 접두사
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil; // JWT 토큰 생성을 위해 주입

    /**
     * 회원가입 API
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequestDto requestDto) {
        userService.signup(requestDto);
        // 성공 시 HTTP 201 Created 상태와 메시지 반환
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공");
    }

    /**
     * 로그인 API
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto requestDto) {
        // 1. UserService에서 로그인 시도 및 토큰 발급
        String token = userService.login(requestDto);

        // 2. 토큰을 Response Header에 담아서 반환
        // (Body에도 메시지를 담아줄 수 있음)
        return ResponseEntity.ok()
                .header(JwtUtil.AUTHORIZATION_HEADER, token)
                .body("로그인 성공");
    }


    /**
     * 구글 로그인 (Swagger 설명용)
     * 이 API를 직접 호출하는 것이 아니라, 브라우저에서 /oauth2/authorization/google 로 이동해야 합니다.
     */
    @GetMapping("/google/login")
    public ResponseEntity<Void> googleLogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/oauth2/authorization/google")
                .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
