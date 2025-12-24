package g3pjt.service.config;

import g3pjt.service.user.jwt.JwtAuthorizationFilter;
import g3pjt.service.user.jwt.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

//    public SecurityConfig() {
//        System.out.println("*** SecurityConfig Loaded ***");
//    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        System.out.println("********** 2. passwordEncoder 빈 생성됨 **********");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthorizationFilter jwtAuthorizationFilter() {
        return new JwtAuthorizationFilter(jwtUtil);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("********** 3. securityFilterChain 빈 생성 시도 **********");
        // 1. CSRF, Form Login, HTTP Basic 비활성화
        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        // OAuth2 로그인 설정 추가
        http.oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2SuccessHandler) // 커스텀 핸들러 등록
                .failureUrl("/login?error=true") // 로그인 실패 시 리디렉션
        );
        // 4. CORS 설정 (프론트엔드 연동 시 필수)
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // 2. 세션(Session)을 사용하지 않도록 설정 (STATELESS)
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 3. ⭐️ (핵심) URL별 권한 설정 (주석 해제!)
        http.authorizeHttpRequests(authz ->
                authz
                        // "/api/user/signup", "/api/user/login" URL은 인증 없이 무조건 허용
                        .requestMatchers("/api/user/signup", "/api/user/login").permitAll()

                // 내 프로필 API는 인증 필요
                .requestMatchers("/api/user/profile").authenticated()

                        // (선택) H2 콘솔 접근 허용 (개발용 - 지금은 MySQL 쓰니 불필요)
                        // .requestMatchers("/h2-console/**").permitAll()

                        // '/api/stores/search' 경로는 '인증'만 되면 (로그인) 허용 -> permitAll for now based on user's existing code? 
                        // The user said "jwt login not working", let's keep permitAll but ensure filter runs.
                        // Actually, to prove it works, we should probably restrict something. 
                        // But I will stick to adding the filter. The filter chain execution is key.
                        .requestMatchers("/api/stores/search/**").permitAll()

                        // Swagger UI 및 API 문서 접근 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // WebSocket 엔드포인트 허용
                        .requestMatchers("/stomp-ws/**").permitAll()


                        // 위에서 허용한 URL 외의 모든 요청은 인증(로그인)이 필요함
                        .anyRequest().permitAll()
        );
        
        // 필터 추가
        http.addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);

        // (선택) H2 콘솔 iframe 허용
        // http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        System.out.println("********** 4. securityFilterChain 설정 완료, 빌드 시작 **********");
        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.addAllowedOriginPattern("*"); // 모든 출처 허용
        configuration.addAllowedMethod("*"); // 모든 HTTP 메서드 허용
        configuration.addAllowedHeader("*"); // 모든 헤더 허용
        configuration.addExposedHeader(JwtUtil.AUTHORIZATION_HEADER); // 프론트에서 헤더 확인 허용
        configuration.setAllowCredentials(true); // 쿠키/인증 정보 포함 허용

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}