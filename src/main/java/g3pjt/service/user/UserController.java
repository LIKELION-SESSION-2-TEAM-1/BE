package g3pjt.service.user;

import g3pjt.service.user.jwt.JwtUtil;
import g3pjt.service.user.userdto.LoginRequestDto;
import g3pjt.service.user.userdto.SignupRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import g3pjt.service.storage.SupabaseStorageService;

@Tag(name = "User API", description = "회원 관련 API")
@RestController // JSON 응답을 위한 컨트롤러
@RequestMapping("/api/user") // 공통 URL 접두사
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SupabaseStorageService supabaseStorageService;

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
    /**
     * 내 프로필 조회
     */
    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 상세 정보를 조회합니다.")
    @GetMapping("/profile")
    public ResponseEntity<g3pjt.service.user.userdto.UserProfileResponse> getMyProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        User user = userService.getUserProfile(username);
        return ResponseEntity.ok(new g3pjt.service.user.userdto.UserProfileResponse(user));
    }

    /**
     * 내 프로필 수정
     */
    @Operation(summary = "내 프로필 수정", description = "현재 로그인한 사용자의 프로필 정보(닉네임, 여행스타일 등)를 수정합니다.")
    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(
            Authentication authentication,
            @RequestBody g3pjt.service.user.userdto.UserUpdateRequest requestDto) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증이 필요합니다.");
        }
        if (requestDto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("요청 본문이 비어 있습니다.");
        }

        String username = authentication.getName();
        userService.updateProfile(username, requestDto);
        return ResponseEntity.ok("프로필 수정 완료");
    }

    @Operation(summary = "프로필 이미지 업로드", description = "Supabase Storage에 프로필 이미지를 업로드하고, 사용자 profileImageUrl을 갱신합니다.")
    @PostMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        String publicUrl = supabaseStorageService.uploadProfileImage(username, file);
        if (publicUrl == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "버킷이 private이면 public URL을 바로 만들 수 없습니다. signed URL 방식으로 변경이 필요합니다."));
        }

        g3pjt.service.user.userdto.UserUpdateRequest request = new g3pjt.service.user.userdto.UserUpdateRequest();
        request.setProfileImageUrl(publicUrl);
        userService.updateProfile(username, request);

        return ResponseEntity.ok(Map.of("profileImageUrl", publicUrl));
    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자를 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<String> deleteAccount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증이 필요합니다.");
        }
        String username = authentication.getName();
        userService.deleteAccount(username);
        return ResponseEntity.ok("회원 탈퇴 완료");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
