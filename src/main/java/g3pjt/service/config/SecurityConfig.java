package g3pjt.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

//    public SecurityConfig() {
//        System.out.println("*** SecurityConfig Loaded ***");
//    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        System.out.println("********** 2. passwordEncoder 빈 생성됨 **********");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("********** 3. securityFilterChain 빈 생성 시도 **********");
        // 1. CSRF, Form Login, HTTP Basic 비활성화
        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        // 2. 세션(Session)을 사용하지 않도록 설정 (STATELESS)
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 3. ⭐️ (핵심) URL별 권한 설정 (주석 해제!)
        http.authorizeHttpRequests(authz ->
                authz
                        // "/api/user/signup", "/api/user/login" URL은 인증 없이 무조건 허용
                        .requestMatchers("/api/user/signup", "/api/user/login").permitAll()

                        // (선택) H2 콘솔 접근 허용 (개발용 - 지금은 MySQL 쓰니 불필요)
                        // .requestMatchers("/h2-console/**").permitAll()

                        // '/api/stores/search' 경로는 '인증'만 되면 (로그인한 사용자면) 허용
                        .requestMatchers("/api/stores/search/**").permitAll()


                        // 위에서 허용한 URL 외의 모든 요청은 인증(로그인)이 필요함
                        .anyRequest().authenticated()
        );

        // (선택) H2 콘솔 iframe 허용
        // http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        System.out.println("********** 4. securityFilterChain 설정 완료, 빌드 시작 **********");
        return http.build();
    }
}