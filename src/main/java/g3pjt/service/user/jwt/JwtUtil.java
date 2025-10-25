package g3pjt.service.user.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component // Spring이 이 객체를 관리하도록(Bean 등록) 설정
public class JwtUtil {

    // 1. Response Header에 사용할 Key 값 (상수)
    public static final String AUTHORIZATION_HEADER = "Authorization";

    // 2. 토큰 앞에 붙일 접두사 (상수)
    public static final String BEARER_PREFIX = "Bearer ";

    // 3. 토큰 만료 시간 (예: 1시간)
    private final long TOKEN_TIME = 60 * 60 * 1000L; // (60분)

    // 4. 서명에 사용할 비밀 키 (application.properties에 저장)
    @Value("${jwt.secret.key}") // (application.properties에서 값 가져오기)
    private String secretKey;

    private Key key; // 서명에 사용할 Key 객체
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    // 5. secretKey를 Base64로 인코딩하여 Key 객체로 변환 (최초 1회)
    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * 6. 토큰 생성 메서드
     */
    public String createToken(String username) {
        Date date = new Date();

        return BEARER_PREFIX + // "Bearer " 접두사 붙이기
                Jwts.builder()
                        .setSubject(username) // (Subject) 토큰에 사용자 이름 저장
                        .setExpiration(new Date(date.getTime() + TOKEN_TIME)) // 만료 시간 설정
                        .setIssuedAt(date) // 발급 시간 설정
                        .signWith(key, signatureAlgorithm) // 비밀 키로 서명
                        .compact();
    }
}
