package g3pjt.service.user.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j(topic = "JWT 검증 및 인가")
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(req);

        if (StringUtils.hasText(token)) {
            if (tokenBlacklistService.isBlacklisted(token)) {
                log.warn("Token is blacklisted");
                filterChain.doFilter(req, res);
                return;
            }
            if (!jwtUtil.validateToken(token)) {
                log.error("Token Error");
                // Don't throw exception, just let it pass as anonymous or return 401?
                // Usually if token is invalid, we might want to block?
                // But for now, let's just log and continue. SecurityContext will be empty, so downstream authorization will fail if required.
            } else {
                try {
                    String username = jwtUtil.getUserFromToken(token);
                    setAuthentication(username);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }

        filterChain.doFilter(req, res);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(JwtUtil.AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(bearerToken)) {
            return null;
        }

        String token = bearerToken.trim();

        // Tolerate common frontend mistakes:
        // - sending raw JWT without 'Bearer '
        // - accidentally prepending 'Bearer ' twice
        for (int i = 0; i < 2; i++) {
            if (token.startsWith(JwtUtil.BEARER_PREFIX)) {
                token = token.substring(JwtUtil.BEARER_PREFIX.length()).trim();
            }
        }

        return token.isEmpty() ? null : token;
    }

    public void setAuthentication(String username) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = createAuthentication(username);
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);
    }

    private Authentication createAuthentication(String username) {
        return new UsernamePasswordAuthenticationToken(username, null, null);
    }
}
