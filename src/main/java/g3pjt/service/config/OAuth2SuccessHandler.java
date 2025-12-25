package g3pjt.service.config;

import g3pjt.service.user.User;
import g3pjt.service.user.UserRepository;
import g3pjt.service.user.jwt.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    private final Set<String> allowedRedirectUris;

    public OAuth2SuccessHandler(
            JwtUtil jwtUtil,
            UserRepository userRepository,
            @org.springframework.beans.factory.annotation.Value("${app.oauth2.redirect-uri-allowlist:http://localhost:3000/home,https://tokplan.vercel.app/home}")
            String allowlist
    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.allowedRedirectUris = Arrays.stream(allowlist.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

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

        // 5. Determine redirect destination from allowlisted redirect_uri (captured in cookie)
        String redirectUri = readRedirectUriCookie(request);

        // Fallback to env FRONTEND_URL if cookie is missing
        if (!StringUtils.hasText(redirectUri)) {
            String frontendUrl = System.getenv("FRONTEND_URL");
            if (!StringUtils.hasText(frontendUrl)) {
                frontendUrl = "https://tokplan.vercel.app";
            }
            redirectUri = frontendUrl + "/home";
        }

        if (!allowedRedirectUris.contains(redirectUri)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid redirect_uri");
            return;
        }

        clearRedirectUriCookie(response);

        // Recommend fragment to reduce referrer/log exposure
        String targetUrl = redirectUri + "#token=" + cleanToken;
        response.sendRedirect(targetUrl);
    }

    private String readRedirectUriCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (OAuth2RedirectUriCookieFilter.REDIRECT_URI_COOKIE.equals(cookie.getName())) {
                String value = cookie.getValue();
                return StringUtils.hasText(value) ? value.trim() : null;
            }
        }
        return null;
    }

    private void clearRedirectUriCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(OAuth2RedirectUriCookieFilter.REDIRECT_URI_COOKIE, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
