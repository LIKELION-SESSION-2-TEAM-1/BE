package g3pjt.service.config;

import g3pjt.service.user.User;
import g3pjt.service.user.UserRepository;
import g3pjt.service.user.jwt.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 1. 구글 로그인 사용자 정보 가져오기
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 2. 이메일 추출 (구글은 "email" 키 사용)
        String email = (String) attributes.get("email");

        // 3. DB에서 사용자 확인 (없으면 회원가입)
        User user = userRepository.findByUsername(email)
                .orElseGet(() -> {
                    // 비밀번호는 OAuth 로그인이라 필요 없지만, DB 제약조건 때문에 임의의 값(UUID) 저장
                    User newUser = new User(email, UUID.randomUUID().toString());
                    return userRepository.save(newUser);
                });

        // 4. JWT 토큰 생성
        String token = jwtUtil.createToken(user.getUsername());
        // "Bearer " 접두사 제거 (URL 파라미터로 전달하기 편하게)
        String cleanToken = token.replace(JwtUtil.BEARER_PREFIX, "");

        // 5. 프론트엔드로 리디렉션 (토큰을 쿼리 파라미터로 전달)
        // 로컬 테스트 시: "http://localhost:5173/?token=" + cleanToken
        // 배포 시: "https://tokplan.vercel.app/?token=" + cleanToken
        String targetUrl = "https://tokplan.vercel.app/?token=" + cleanToken;
        
        response.sendRedirect(targetUrl);
    }
}
