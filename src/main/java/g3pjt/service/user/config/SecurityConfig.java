package g3pjt.service.config; // ⭐️ 패키지 경로가 맞는지 확인!

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // Spring Security 설정을 활성화합니다.
public class SecurityConfig {

    /**
     * 비밀번호 암호화 (BCrypt)
     * UserService에서 사용할 수 있도록 Bean으로 등록
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * ⭐️ 핵심: 보안 필터 체인 설정
     * 이 코드가 Spring Security의 "공장 초기값"을 덮어씁니다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1. CSRF 보호 비활성화
        http.csrf(AbstractHttpConfigurer::disable);

        // 2. ⭐️ /login 리디렉션을 유발하는 Form Login 비활성화
        http.formLogin(AbstractHttpConfigurer::disable);

        // (기타: HTTP Basic 인증, 세션 관리 설정)
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

//        // 3. ⭐️ URL별 권한 설정
//        http.authorizeHttpRequests(authz ->
//                authz
//                        // "/api/user/signup" 과 "/api/user/login" URL은 인증 없이 무조건 허용
//                        .requestMatchers("/api/user/signup", "/api/user/login").permitAll()
//
//                        // (선택) H2 콘솔 접근 허용 (개발용)
//                        .requestMatchers("/h2-console/**").permitAll()
//
//                        // 위에서 허용한 URL 외의 모든 요청은 인증(로그인)이 필요함
//                        .anyRequest().authenticated()
//        );

        // (선택) H2 콘솔 iframe 허용
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}