package g3pjt.service.user.jwt;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Map<String, Long> tokenToExpiryMillis = new ConcurrentHashMap<>();

    public void blacklist(String token, Date expiresAt) {
        if (token == null || token.isBlank() || expiresAt == null) {
            return;
        }
        long expiryMillis = expiresAt.getTime();
        long now = System.currentTimeMillis();
        if (expiryMillis <= now) {
            return;
        }

        tokenToExpiryMillis.put(token, expiryMillis);
        cleanupExpired(now);
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        Long expiryMillis = tokenToExpiryMillis.get(token);
        if (expiryMillis == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (expiryMillis <= now) {
            tokenToExpiryMillis.remove(token);
            return false;
        }

        return true;
    }

    private void cleanupExpired(long now) {
        // 간단한 정리: 호출 시점에 만료된 항목만 제거
        tokenToExpiryMillis.entrySet().removeIf(e -> e.getValue() == null || e.getValue() <= now);
    }
}
