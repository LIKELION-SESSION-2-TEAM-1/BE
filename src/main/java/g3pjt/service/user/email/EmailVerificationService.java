package g3pjt.service.user.email;

import g3pjt.service.user.User;
import g3pjt.service.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration DEFAULT_TOKEN_TTL = Duration.ofMinutes(30);

    private final JavaMailSender mailSender;
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Value("${FRONTEND_URL:}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Transactional
    public void sendVerificationEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }

        sendVerificationEmailByUsername(authentication.getName());
    }

    @Transactional
    public void sendVerificationEmailByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username이 비어있습니다.");
        }

        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("이미 인증된 이메일입니다.");
        }

        // username이 이메일이 아닌 경우엔 인증 메일 대상에서 제외
        if (user.getUsername() == null || !user.getUsername().contains("@")) {
            throw new IllegalArgumentException("이메일 형식의 username만 인증할 수 있습니다.");
        }

        String token = generateToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(DEFAULT_TOKEN_TTL);

        tokenRepository.save(new EmailVerificationToken(user, token, now, expiresAt));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getUsername());

        // From은 비어있을 수 있어요(환경변수 미설정). 비어있으면 JavaMailSender 기본값 사용.
        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }

        message.setSubject("[TOKPLAN] 이메일 인증 안내");
        message.setText(buildBody(token));

        mailSender.send(message);
    }

    @Transactional
    public void confirm(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("token이 비어있습니다.");
        }

        Instant now = Instant.now();
        EmailVerificationToken record = tokenRepository.findByToken(token.trim())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 token 입니다."));

        if (record.isUsed()) {
            throw new IllegalArgumentException("이미 사용된 token 입니다.");
        }
        if (record.isExpired(now)) {
            throw new IllegalArgumentException("만료된 token 입니다.");
        }

        User user = record.getUser();
        user.markEmailVerified(now);
        record.markUsed(now);

        // JPA dirty checking으로 업데이트
    }

    private String buildBody(String token) {
        String link = null;
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            String base = frontendUrl.trim();
            link = base + "/verify-email?token=" + token;
        }

        if (link != null) {
            return "안녕하세요.\n\n아래 링크를 눌러 이메일 인증을 완료해주세요.\n\n" + link + "\n\n" +
                    "만약 링크가 동작하지 않으면 token을 복사해 인증 화면에 붙여넣으세요:\n" + token + "\n\n" +
                    "이 토큰은 30분 후 만료됩니다.";
        }

        return "안녕하세요.\n\n아래 token을 인증 화면에 입력해 이메일 인증을 완료해주세요.\n\n" + token + "\n\n" +
                "이 토큰은 30분 후 만료됩니다.";
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        // URL-safe 토큰
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
