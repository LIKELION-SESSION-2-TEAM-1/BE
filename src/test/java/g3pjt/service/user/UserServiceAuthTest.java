package g3pjt.service.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import g3pjt.service.favorite.FavoritePlaceRepository;
import g3pjt.service.search.RecentSearchRepository;
import g3pjt.service.user.email.EmailVerificationService;
import g3pjt.service.user.email.EmailVerificationTokenRepository;
import g3pjt.service.user.jwt.JwtUtil;
import g3pjt.service.user.userdto.LoginRequestDto;
import g3pjt.service.user.userdto.SignupRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserServiceAuthTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void signup_rejects_nonEmail_username() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        FavoritePlaceRepository favoritePlaceRepository = mock(FavoritePlaceRepository.class);
        RecentSearchRepository recentSearchRepository = mock(RecentSearchRepository.class);
        EmailVerificationTokenRepository emailVerificationTokenRepository = mock(EmailVerificationTokenRepository.class);
        EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);

        UserService userService = new UserService(
                userRepository,
                favoritePlaceRepository,
                recentSearchRepository,
                emailVerificationTokenRepository,
                emailVerificationService,
                passwordEncoder,
                jwtUtil
        );

        SignupRequestDto dto = objectMapper.readValue(
                "{\"username\":\"not-an-email\",\"password\":\"pw\"}",
                SignupRequestDto.class
        );

        assertThrows(IllegalArgumentException.class, () -> userService.signup(dto));
        verify(userRepository, never()).save(any());
        verify(emailVerificationService, never()).sendVerificationEmailByUsername(anyString());
    }

    @Test
    void login_blocks_unverified_email_account() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        FavoritePlaceRepository favoritePlaceRepository = mock(FavoritePlaceRepository.class);
        RecentSearchRepository recentSearchRepository = mock(RecentSearchRepository.class);
        EmailVerificationTokenRepository emailVerificationTokenRepository = mock(EmailVerificationTokenRepository.class);
        EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);

        UserService userService = new UserService(
                userRepository,
                favoritePlaceRepository,
                recentSearchRepository,
                emailVerificationTokenRepository,
                emailVerificationService,
                passwordEncoder,
                jwtUtil
        );

        User user = new User("user@example.com", "encoded");
        when(userRepository.findByUsername("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("pw", "encoded")).thenReturn(true);

        LoginRequestDto dto = objectMapper.readValue(
                "{\"username\":\"user@example.com\",\"password\":\"pw\"}",
                LoginRequestDto.class
        );

        assertThrows(EmailVerificationRequiredException.class, () -> userService.login(dto));
        verify(jwtUtil, never()).createToken(anyString());
    }
}
