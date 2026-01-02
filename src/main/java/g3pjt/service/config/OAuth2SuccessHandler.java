package g3pjt.service.config;

import g3pjt.service.user.User;
import g3pjt.service.user.UserRepository;
import g3pjt.service.user.jwt.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 1. 서비스 제공자 구분 (google, kakao, naver)
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        String username;

        if ("kakao".equals(registrationId)) {
            // 카카오는 고유 번호 id 사용
            username = attributes.get("id").toString();
        } else if ("naver".equals(registrationId)) {
            // 네이버는 response 맵 내부의 id 사용
            Map<String, Object> naverResponse = (Map<String, Object>) attributes.get("response");
            username = (String) naverResponse.get("id");
        } else {
            // 기본값 (구글 등): 기존 방식대로 email 사용
            username = (String) attributes.get("email");
        }
        System.out.println(username);

        // 2. DB 확인 (기존 변수명과 로직 그대로 유지)
        final String finalUsername = username;
        User user = userRepository.findByUsername(finalUsername)
                .orElseGet(() -> {
                    System.out.println("신규 유저입니다. 저장을 시작합니다.");
                    // 기존 User 생성자가 (String, String) 구조인지 확인하세요.
                    User newUser = new User(finalUsername, UUID.randomUUID().toString());
                    return userRepository.save(newUser);
                });

        // 3. JWT 및 리다이렉트 처리 (기존 코드와 100% 동일)
        String token = jwtUtil.createToken(user.getUsername());
        String cleanToken = token.replace(JwtUtil.BEARER_PREFIX, "");

        String redirectUri = readRedirectUriCookie(request);

        if (!StringUtils.hasText(redirectUri)) {
            String frontendUrl = System.getenv("FRONTEND_URL");
            if (!StringUtils.hasText(frontendUrl)) {
                // 로컬 테스트 중이라면 3000포트 주소를 여기에 넣으세요.
                frontendUrl = "http://localhost:3000";
            }
            redirectUri = frontendUrl + "/home";
        }

        // 기존의 allowlist 검증 로직 유지
        if (!allowedRedirectUris.contains(redirectUri)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid redirect_uri: " + redirectUri);
            return;
        }

        clearRedirectUriCookie(response);

        String targetUrl = redirectUri + "?token=" + cleanToken;
        System.out.println("최종 리다이렉트 주소: " + targetUrl);
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
