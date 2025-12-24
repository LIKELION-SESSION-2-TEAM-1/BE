package g3pjt.service.user;

// ... (다른 import)
import g3pjt.service.user.jwt.JwtUtil; // ⭐️ JwtUtil 임포트
import g3pjt.service.user.userdto.LoginRequestDto;
import g3pjt.service.user.userdto.SignupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 만들어줍니다.
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 회원가입
     */
    public void signup(SignupRequestDto requestDto) {
        String username = requestDto.getUsername();
        String password = requestDto.getPassword();

        // 1. 아이디 중복 확인
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        // 3. 유저 생성 및 저장
        User user = new User(username, encodedPassword);
        user.setNickname(username); // 초기 닉네임은 아이디와 동일하게 설정
        userRepository.save(user);
    }

    /**
     * 로그인
     */
    public String login(LoginRequestDto requestDto) {
        String username = requestDto.getUsername();
        String password = requestDto.getPassword();

        // 1. 유저 확인
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.")
        );

        // 2. 비밀번호 대조
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 3. 로그인 성공 및 JWT 발급
        return jwtUtil.createToken(user.getUsername());
    }
    public String getDisplayNameByUserId(Long userId){
        if (userId == null) return "unknown";
        return userRepository.findById(userId)
                .map(user -> {
                    // 닉네임이 있으면 닉네임 반환, 없으면 유저네임 반환
                    if (user.getNickname() != null && !user.getNickname().isEmpty()) {
                        return user.getNickname();
                    }
                    return user.getUsername();
                }).orElse("unknown");
    }

    /**
     * 프로필 수정
     */
    @org.springframework.transaction.annotation.Transactional
    public void updateProfile(String username, g3pjt.service.user.userdto.UserUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String birthDate = normalizeBirthDate(request.getBirthDate());

        user.updateProfile(
                request.getNickname(),
                request.getProfileImageUrl(),
                birthDate,
                request.getTravelPace(),
                request.getDailyRhythm(),
                request.getFoodPreferences(),
                request.getFoodRestrictions()
        );
        // Transactional 어노테이션 덕분에 save 호출 없이도 더티 체킹으로 업데이트됨
    }

    private String normalizeBirthDate(String birthDate) {
        if (birthDate == null) {
            return null;
        }
        String trimmed = birthDate.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // Expect ISO local date: YYYY-MM-DD
        try {
            LocalDate parsed = LocalDate.parse(trimmed);
            return parsed.toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("생년월일 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 보내주세요.");
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        userRepository.delete(user);
    }

    /**
     * 프로필 조회
     */
    public User getUserProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    /**
     * identifier로 사용자 조회
     * - email/username: user.username (대부분 이메일 또는 아이디)
     * - nickname: user.nickname
     */
    public User findByUsernameOrNickname(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 식별자가 비어있습니다.");
        }
        String trimmed = identifier.trim();

        // 이메일 형태면 username으로 우선 조회
        if (trimmed.contains("@")) {
            return userRepository.findByUsername(trimmed)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        }

        Optional<User> byNickname = userRepository.findByNickname(trimmed);
        if (byNickname.isPresent()) {
            return byNickname.get();
        }

        return userRepository.findByUsername(trimmed)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}