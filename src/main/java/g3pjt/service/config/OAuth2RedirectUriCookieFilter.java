package g3pjt.service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OAuth2RedirectUriCookieFilter extends OncePerRequestFilter {

    public static final String REDIRECT_URI_PARAM = "redirect_uri";
    public static final String REDIRECT_URI_COOKIE = "oauth2_redirect_uri";

    private final Set<String> allowedRedirectUris;

    public OAuth2RedirectUriCookieFilter(
            @Value("${app.oauth2.redirect-uri-allowlist:http://localhost:3000/home,https://tokplan.vercel.app/home}")
            String allowlist
    ) {
        this.allowedRedirectUris = Arrays.stream(allowlist.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/oauth2/authorization/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String redirectUri = request.getParameter(REDIRECT_URI_PARAM);
        if (StringUtils.hasText(redirectUri)) {
            String trimmed = redirectUri.trim();
            if (!allowedRedirectUris.contains(trimmed)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid redirect_uri");
                return;
            }

            Cookie cookie = new Cookie(REDIRECT_URI_COOKIE, trimmed);
            cookie.setPath("/");
            cookie.setMaxAge(180); // 3 minutes
            // OAuth2 success handler will read and then clear this cookie.
            response.addCookie(cookie);
        }

        filterChain.doFilter(request, response);
    }
}
